package com.ryvenca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.ryvenca.firebase.FirebaseAuthGateway;
import com.ryvenca.palette.PaletteService;

@SpringBootTest
@AutoConfigureMockMvc
class AccountAndAdminIntegrationTest {

    /** Accepts tokens of the form {@code uid|email|provider|verified}. */
    static class FakeFirebase implements FirebaseAuthGateway {
        final List<String> deleted = new CopyOnWriteArrayList<>();

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public Optional<String> projectId() {
            return Optional.of("ryvenca-test");
        }

        @Override
        public VerifiedToken verify(String idToken) {
            String[] p = idToken.split("\\|");
            if (p.length != 4) {
                throw new InvalidTokenException("bad token", null);
            }
            return new VerifiedToken(p[0], p[1], Boolean.parseBoolean(p[3]), null, p[2]);
        }

        @Override
        public void deleteUser(String uid) {
            deleted.add(uid);
        }
    }

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        FakeFirebase fakeFirebase() {
            return new FakeFirebase();
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        Path media = Files.createTempDirectory("ryvenca-media");
        registry.add("ryvenca.storage.local-dir", media::toString);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:account-admin;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
        registry.add("ryvenca.admin.emails", () -> "boss@example.com");
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    FakeFirebase firebase;

    @Autowired
    PaletteService palettes;

    @Test
    void firebaseSignInCreatesAndLinksAccounts() throws Exception {
        String first = firebaseLogin("uid-google-1|ayse@example.com|google.com|true", "Ayşe");
        mvc.perform(auth(get("/api/me"), first))
                .andExpect(jsonPath("$.authProvider").value("GOOGLE"))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.displayName").value("Ayşe"));
        long id = userId(first);
        // Same Firebase user again → same account.
        assertThat(userId(firebaseLogin("uid-google-1|ayse@example.com|google.com|true", null))).isEqualTo(id);
        // Same e-mail through Apple → linked to the same account.
        assertThat(userId(firebaseLogin("uid-apple-9|ayse@example.com|apple.com|true", null))).isEqualTo(id);

        // Unverified password account must not take over an existing e-mail.
        mvc.perform(post("/api/auth/firebase").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"uid-x|ayse@example.com|password|false\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/firebase").header("Accept-Language", "en").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"garbage\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Sign-in could not be verified. Please sign in again."));
        // Name falls back to the e-mail's local part.
        String anon = firebaseLogin("uid-2|zeynep.k@example.com|password|false", null);
        mvc.perform(auth(get("/api/me"), anon)).andExpect(jsonPath("$.displayName").value("Zeynep.k"))
                .andExpect(jsonPath("$.authProvider").value("PASSWORD"));
    }

    @Test
    void publicConfigAndAdminSettings() throws Exception {
        mvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auth.firebase").value(true))
                .andExpect(jsonPath("$.auth.local").value(true))
                .andExpect(jsonPath("$.auth.providers.apple").value(true))
                .andExpect(jsonPath("$.firebaseWeb").doesNotExist())
                .andExpect(jsonPath("$.maintenance.enabled").value(false));

        String user = firebaseLogin("uid-u|user@example.com|google.com|true", null);
        mvc.perform(auth(get("/api/admin/stats"), user)).andExpect(status().isForbidden());

        String admin = firebaseLogin("uid-boss|boss@example.com|google.com|true", null);
        mvc.perform(auth(get("/api/me"), admin)).andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(auth(get("/api/admin/settings"), admin))
                .andExpect(jsonPath("$.status.firebaseAdmin").value(true))
                .andExpect(jsonPath("$.status.firebaseProjectId").value("ryvenca-test"));

        mvc.perform(auth(put("/api/admin/settings"), admin).header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"links\":{\"privacyPolicy\":\"not a url\",\"supportEmail\":\"nope\"},\"minVersion\":{\"ios\":\"x\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['links.privacyPolicy']").value("Enter a valid address starting with http:// or https://"))
                .andExpect(jsonPath("$.fieldErrors['links.supportEmail']").exists())
                .andExpect(jsonPath("$.fieldErrors['minVersion.ios']").exists());

        mvc.perform(auth(put("/api/admin/settings"), admin).contentType(MediaType.APPLICATION_JSON).content("""
                        {"providers":{"apple":false},
                         "firebaseWeb":{"apiKey":"AIza-test","authDomain":"ryvenca.firebaseapp.com","projectId":"ryvenca","appId":"1:2:web:3"},
                         "links":{"privacyPolicy":"https://ryvenca.com/privacy","accountDeletion":"https://ryvenca.com/delete-account","supportEmail":"help@ryvenca.com"},
                         "maintenance":{"enabled":true,"message":"Back soon"},
                         "minVersion":{"ios":"1.2.0"}}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links.privacyPolicy").value("https://ryvenca.com/privacy"));
        mvc.perform(get("/api/config"))
                .andExpect(jsonPath("$.auth.providers.apple").value(false))
                .andExpect(jsonPath("$.auth.providers.google").value(true))
                .andExpect(jsonPath("$.firebaseWeb.projectId").value("ryvenca"))
                .andExpect(jsonPath("$.links.supportEmail").value("help@ryvenca.com"))
                .andExpect(jsonPath("$.maintenance.enabled").value(true))
                .andExpect(jsonPath("$.maintenance.message").value("Back soon"))
                .andExpect(jsonPath("$.minVersion.ios").value("1.2.0"));
        // Empty string clears a value.
        mvc.perform(auth(put("/api/admin/settings"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maintenance\":{\"enabled\":false,\"message\":\"\"}}"))
                .andExpect(jsonPath("$.maintenance.message").doesNotExist());
    }

    @Test
    void adminManagesUsers() throws Exception {
        String admin = firebaseLogin("uid-boss|boss@example.com|google.com|true", null);
        long adminId = userId(admin);
        String member = firebaseLogin("uid-m|member@example.com|apple.com|true", "Member");
        long memberId = userId(member);

        mvc.perform(auth(get("/api/admin/users").param("q", "MEMBER@"), admin))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].authProvider").value("APPLE"))
                .andExpect(jsonPath("$.items[0].garmentCount").value(0));

        mvc.perform(auth(patch("/api/admin/users/" + adminId), admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disabled\":true}"))
                .andExpect(status().isBadRequest());

        mvc.perform(auth(patch("/api/admin/users/" + memberId), admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disabled\":true}"))
                .andExpect(jsonPath("$.disabled").value(true));
        mvc.perform(auth(get("/api/home"), member).header("Accept-Language", "en"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCOUNT_DISABLED"));
        mvc.perform(post("/api/auth/firebase").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"uid-m|member@example.com|apple.com|true\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(auth(patch("/api/admin/users/" + memberId), admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disabled\":false,\"role\":\"ADMIN\"}"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(auth(get("/api/admin/stats"), member)).andExpect(status().isOk());

        mvc.perform(auth(delete("/api/admin/users/" + memberId), admin)).andExpect(status().isNoContent());
        assertThat(firebase.deleted).contains("uid-m");
        mvc.perform(auth(get("/api/admin/deletion-log"), admin))
                .andExpect(jsonPath("$[0].method").value("ADMIN"))
                .andExpect(jsonPath("$[0].authProvider").value("APPLE"));
    }

    @Test
    void selfServiceDeletionFromTheApp() throws Exception {
        String token = firebaseLogin("uid-del|leaving@example.com|apple.com|true", null);
        mvc.perform(auth(delete("/api/me"), token).header("X-Client", "mobile/1.0.0")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Not for me\"}"))
                .andExpect(status().isNoContent());
        assertThat(firebase.deleted).contains("uid-del");
        mvc.perform(auth(get("/api/me"), token)).andExpect(status().isUnauthorized());
        String admin = firebaseLogin("uid-boss|boss@example.com|google.com|true", null);
        String log = mvc.perform(auth(get("/api/admin/deletion-log"), admin)).andReturn().getResponse().getContentAsString();
        List<String> methods = JsonPath.read(log, "$[?(@.reason == 'Not for me')].method");
        assertThat(methods).containsExactly("IN_APP");
        assertThat(log).doesNotContain("leaving@example.com");
    }

    @Test
    void deletionRequestsAreReviewedByAnAdmin() throws Exception {
        String victim = firebaseLogin("uid-lost|lost@example.com|google.com|true", null);
        long victimId = userId(victim);

        String first = mvc.perform(post("/api/account-deletion-requests").with(r -> { r.setRemoteAddr("10.0.0.1"); return r; })
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"Lost@Example.com\",\"message\":\"Lost my phone\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.reference").value(org.hamcrest.Matchers.matchesPattern("DR-[A-Z0-9]{6}")))
                .andReturn().getResponse().getContentAsString();
        String again = mvc.perform(post("/api/account-deletion-requests").with(r -> { r.setRemoteAddr("10.0.0.1"); return r; })
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"lost@example.com\"}"))
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(again, "$.reference")).isEqualTo(JsonPath.read(first, "$.reference"));
        // Unknown e-mails get the same answer (no account enumeration).
        mvc.perform(post("/api/account-deletion-requests").with(r -> { r.setRemoteAddr("10.0.0.1"); return r; })
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isAccepted());

        String admin = firebaseLogin("uid-boss|boss@example.com|google.com|true", null);
        String pending = mvc.perform(auth(get("/api/admin/deletion-requests").param("status", "PENDING"), admin))
                .andExpect(jsonPath("$", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        Integer lostId = JsonPath.<List<Integer>>read(pending, "$[?(@.email == 'lost@example.com')].id").getFirst();
        List<Integer> matched = JsonPath.read(pending, "$[?(@.email == 'lost@example.com')].matchedUserId");
        assertThat(matched).containsExactly((int) victimId);
        Integer nobodyId = JsonPath.<List<Integer>>read(pending, "$[?(@.email == 'nobody@example.com')].id").getFirst();

        mvc.perform(auth(post("/api/admin/deletion-requests/" + nobodyId + "/reject"), admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(post("/api/admin/deletion-requests/" + nobodyId + "/reject"), admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"No such account\"}"))
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mvc.perform(auth(post("/api/admin/deletion-requests/" + lostId + "/approve"), admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Verified by mail\"}"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(auth(get("/api/me"), victim)).andExpect(status().isUnauthorized());
        mvc.perform(auth(post("/api/admin/deletion-requests/" + lostId + "/approve"), admin))
                .andExpect(status().isConflict());
    }

    @Test
    void publicDeletionRequestsAreRateLimited() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/account-deletion-requests").with(r -> { r.setRemoteAddr("10.9.9.9"); return r; })
                            .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"spam" + i + "@example.com\"}"))
                    .andExpect(status().isAccepted());
        }
        mvc.perform(post("/api/account-deletion-requests").with(r -> { r.setRemoteAddr("10.9.9.9"); return r; })
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"spam9@example.com\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void adminEditsPalettesUsedByTheEngine() throws Exception {
        String admin = firebaseLogin("uid-boss|boss@example.com|google.com|true", null);
        mvc.perform(auth(get("/api/admin/palettes"), admin))
                .andExpect(jsonPath("$", hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(45))))
                .andExpect(jsonPath("$[0].id").value("P001"))
                .andExpect(jsonPath("$[0].builtIn").value(true))
                .andExpect(jsonPath("$[0].names.tr").value("Toprak Tonları"))
                .andExpect(jsonPath("$[0].names.ja").exists());
        int before = palettes.library().size();

        String created = mvc.perform(auth(post("/api/admin/palettes"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"colors\":[\"#112233\",\"#F5F5F2\"],\"names\":{\"en\":\"Ink\",\"tr\":\"Mürekkep\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.builtIn").value(false))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");
        assertThat(palettes.library().size()).isEqualTo(before + 1);

        mvc.perform(auth(put("/api/admin/palettes/P001"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false,\"names\":{\"tr\":\"Toprak\"}}"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.names.tr").value("Toprak"))
                .andExpect(jsonPath("$.names.en").value("Earth Tones"));
        assertThat(palettes.library().palettes()).noneMatch(p -> p.id().equals("P001"));

        mvc.perform(auth(post("/api/admin/palettes"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"colors\":[\"red\"],\"names\":{\"en\":\"x\"}}"))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(delete("/api/admin/palettes/P002"), admin)).andExpect(status().isBadRequest());
        mvc.perform(auth(delete("/api/admin/palettes/" + id), admin)).andExpect(status().isNoContent());
        mvc.perform(auth(put("/api/admin/palettes/P001"), admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":true,\"names\":{}}")).andExpect(status().isOk());
    }

    private String firebaseLogin(String idToken, String displayName) throws Exception {
        String body = mvc.perform(post("/api/auth/firebase").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"" + idToken + "\"" + (displayName == null ? "" : ",\"displayName\":\"" + displayName + "\"") + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private long userId(String token) throws Exception {
        String me = mvc.perform(auth(get("/api/me"), token)).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(me, "$.id")).longValue();
    }

    private static <B extends AbstractMockHttpServletRequestBuilder<B>> B auth(B builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }
}
