package com.ryvenca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.ryvenca.color.SyntheticPhotos;
import com.ryvenca.image.ImageProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class ApiFlowIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        Path media = Files.createTempDirectory("ryvenca-media");
        registry.add("ryvenca.storage.local-dir", media::toString);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:api-flow;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
    }

    @Autowired
    MockMvc mvc;

    private final ImageProcessor processor = new ImageProcessor();

    @Test
    void metaIsPublicAndLabelled() throws Exception {
        mvc.perform(get("/api/meta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].code").value("TOP"))
                .andExpect(jsonPath("$.categories[0].label").value("Üst"))
                .andExpect(jsonPath("$.categories[0].subcategories[0].code").value("T_SHIRT"))
                .andExpect(jsonPath("$.colors", hasSize(17)))
                .andExpect(jsonPath("$.styles[1].label").value("Smart Casual"))
                .andExpect(jsonPath("$.occasions[1].label").value("Ofis"));
    }

    @Test
    void protectedEndpointsRequireAToken() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Lütfen giriş yap."));
        mvc.perform(get("/api/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationValidationAndLogin() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad\",\"password\":\"short\",\"displayName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.email").value("Geçerli bir e-posta gir"))
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        register("Deniz@Example.com", "Deniz");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"deniz@example.com\",\"password\":\"password123\",\"displayName\":\"D\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"deniz@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-posta veya şifre hatalı."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"DENIZ@example.com \",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("deniz@example.com"));
    }

    @Test
    void fullWardrobeFlow() throws Exception {
        String token = register("ayse@example.com", "Ayşe");

        // Profile & onboarding
        mvc.perform(auth(put("/api/me"), token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wardrobeType\":\"WOMEN\",\"stylePreferences\":[\"MINIMAL\",\"CLASSIC\"],\"onboardingCompleted\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wardrobeType").value("WOMEN"))
                .andExpect(jsonPath("$.stylePreferences", hasSize(2)))
                .andExpect(jsonPath("$.onboardingCompleted").value(true));

        // Upload + automatic color detection
        String upload = upload(token, "#F2EEE8", "#1F2A44");
        assertThat(JsonPath.<String>read(upload, "$.detection.color")).isEqualTo("NAVY");
        assertThat(JsonPath.<String>read(upload, "$.imageUrl")).startsWith("http://localhost/media/").endsWith("-display.jpg");
        String thumb = JsonPath.read(upload, "$.thumbnailUrl");
        mvc.perform(get(thumb.replace("http://localhost", "")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));

        long shirt = createGarment(token, JsonPath.read(upload, "$.imageId"), "TOP", "SHIRT", "NAVY", null);
        String shirtJson = mvc.perform(auth(get("/api/garments/" + shirt), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Lacivert Gömlek"))
                .andExpect(jsonPath("$.colorSource").value("AUTO"))
                .andExpect(jsonPath("$.seasons", hasSize(4)))
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(shirtJson, "$.colorHex")).isNotEqualTo("#1F2A44");

        // Manual override: the user says it is black
        String beigeUpload = upload(token, "#8A6A4F", "#C8B596");
        long trousers = createGarment(token, JsonPath.read(beigeUpload, "$.imageId"), "BOTTOM", "TROUSERS", "BLACK", "Siyah pantolonum");
        mvc.perform(auth(get("/api/garments/" + trousers), token))
                .andExpect(jsonPath("$.color").value("BLACK"))
                .andExpect(jsonPath("$.colorHex").value("#1C1C1C"))
                .andExpect(jsonPath("$.colorSource").value("MANUAL"))
                .andExpect(jsonPath("$.displayName").value("Siyah pantolonum"));

        // A subcategory from another category is rejected
        String badUpload = upload(token, "#F2EEE8", "#1C1C1C");
        mvc.perform(auth(post("/api/garments"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(garmentJson(JsonPath.read(badUpload, "$.imageId"), "TOP", "JEANS", "BLACK", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.subcategory").exists());

        // Not ready yet: no shoes
        mvc.perform(auth(get("/api/outfits/suggestions"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readiness.ready").value(false))
                .andExpect(jsonPath("$.readiness.missing[0].category").value("SHOES"))
                .andExpect(jsonPath("$.outfits", hasSize(0)));

        long loafer = createGarment(token, JsonPath.read(upload(token, "#EFEFEF", "#5E3B24"), "$.imageId"), "SHOES", "LOAFER", "BROWN", null);
        long blazer = createGarment(token, JsonPath.read(upload(token, "#F2EEE8", "#C8B596"), "$.imageId"), "OUTERWEAR", "BLAZER", "BEIGE", null);
        createGarment(token, JsonPath.read(upload(token, "#D9D5CF", "#4F6A8E"), "$.imageId"), "BOTTOM", "JEANS", "BLUE", null);
        createGarment(token, JsonPath.read(upload(token, "#8A6A4F", "#F4F3EF"), "$.imageId"), "SHOES", "SNEAKER", "WHITE", null);

        String suggestions = mvc.perform(auth(get("/api/outfits/suggestions").param("season", "AUTUMN").param("limit", "5"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readiness.ready").value(true))
                .andExpect(jsonPath("$.season").value("AUTUMN"))
                .andExpect(jsonPath("$.outfits[0].score").isNumber())
                .andExpect(jsonPath("$.outfits[0].reasons[0].title").value("Renk Dengesi"))
                .andExpect(jsonPath("$.outfits[0].breakdown", hasSize(5)))
                .andExpect(jsonPath("$.outfits[0].items[0].garment.thumbnailUrl", startsWith("http://localhost/media/")))
                .andReturn().getResponse().getContentAsString();
        List<Integer> firstIds = JsonPath.read(suggestions, "$.outfits[0].items[*].garment.id");
        assertThat(firstIds).isNotEmpty();

        // Evaluate a specific outfit
        String items = shirt + "," + trousers + "," + loafer + "," + blazer;
        mvc.perform(auth(get("/api/outfits/evaluate").param("items", items), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].role").value("OUTERWEAR"))
                .andExpect(jsonPath("$.items", hasSize(4)))
                .andExpect(jsonPath("$.venues").isArray())
                .andExpect(jsonPath("$.palette[0].hex", startsWith("#")))
                .andExpect(jsonPath("$.saved").value(false));
        mvc.perform(auth(get("/api/outfits/evaluate").param("items", shirt + "," + trousers), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("ayakkabı")));

        // Save (idempotent), list, similar
        String saved = mvc.perform(auth(post("/api/outfits/saved"), token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"garmentIds\":[" + items + "]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saved").value(true))
                .andReturn().getResponse().getContentAsString();
        Integer savedId = JsonPath.read(saved, "$.savedId");
        mvc.perform(auth(post("/api/outfits/saved"), token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"garmentIds\":[" + items + "]}"))
                .andExpect(jsonPath("$.savedId").value(savedId));
        mvc.perform(auth(get("/api/outfits/saved"), token))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].savedId").value(savedId));
        mvc.perform(auth(get("/api/outfits/similar").param("items", items), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outfits").isArray());

        // "Bununla ne gider?"
        mvc.perform(auth(get("/api/garments/" + blazer + "/pairings"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anchor.id").value(blazer))
                .andExpect(jsonPath("$.matches[0].label").isNotEmpty())
                .andExpect(jsonPath("$.outfits[0].items[*].garment.id", org.hamcrest.Matchers.hasItem((int) blazer)));

        // Favorites
        mvc.perform(auth(put("/api/garments/" + blazer + "/favorite"), token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"favorite\":true}"))
                .andExpect(jsonPath("$.favorite").value(true));
        mvc.perform(auth(get("/api/garments").param("favorite", "true"), token))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(auth(get("/api/garments").param("category", "BOTTOM,SHOES"), token))
                .andExpect(jsonPath("$", hasSize(4)));
        mvc.perform(auth(get("/api/garments").param("q", "gömlek"), token))
                .andExpect(jsonPath("$", hasSize(1)));

        // Home
        mvc.perform(auth(get("/api/home"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.garmentCount").value(6))
                .andExpect(jsonPath("$.stats.favoriteCount").value(2))
                .andExpect(jsonPath("$.stats.readyOutfitCount").isNumber())
                .andExpect(jsonPath("$.recentGarments", hasSize(6)))
                .andExpect(jsonPath("$.readiness.ready").value(true));

        // Another user cannot see these garments
        String other = register("mehmet@example.com", "Mehmet");
        mvc.perform(auth(get("/api/garments/" + shirt), other)).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/outfits/saved/" + savedId), other)).andExpect(status().isNotFound());

        // Deleting a garment removes saved outfits that contain it
        mvc.perform(auth(delete("/api/garments/" + blazer), token)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/outfits/saved"), token)).andExpect(jsonPath("$", hasSize(0)));

        // Account deletion
        mvc.perform(auth(delete("/api/me"), token)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/me"), token)).andExpect(status().isUnauthorized());
    }

    @Test
    void speaksTheRequestLanguage() throws Exception {
        mvc.perform(get("/api/meta").header("Accept-Language", "en-US,en;q=0.9"))
                .andExpect(jsonPath("$.categories[0].label").value("Top"))
                .andExpect(jsonPath("$.colors[4].label").value("Beige"))
                .andExpect(jsonPath("$.languages", hasSize(16)))
                .andExpect(jsonPath("$.languages[5].code").value("ar"))
                .andExpect(jsonPath("$.languages[5].rtl").value(true));
        mvc.perform(get("/api/me").header("Accept-Language", "en"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please sign in."));
        mvc.perform(post("/api/auth/register").header("Accept-Language", "en").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad\",\"password\":\"password123\",\"displayName\":\"X\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").value("Enter a valid email address"));

        String token = register("lang@example.com", "Lang");
        mvc.perform(auth(put("/api/me"), token).contentType(MediaType.APPLICATION_JSON).content("{\"language\":\"ja-JP\"}"))
                .andExpect(jsonPath("$.language").value("ja"));
        mvc.perform(auth(put("/api/me"), token).contentType(MediaType.APPLICATION_JSON).content("{\"language\":\"xx\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.language").exists());

        String upload = upload(token, "#F2EEE8", "#C8B596");
        long blazer = createGarment(token, JsonPath.read(upload, "$.imageId"), "OUTERWEAR", "BLAZER", "BEIGE", null);
        mvc.perform(auth(get("/api/garments/" + blazer), token).header("Accept-Language", "en"))
                .andExpect(jsonPath("$.displayName").value("Beige blazer"));
        mvc.perform(auth(get("/api/garments/" + blazer), token))
                .andExpect(jsonPath("$.displayName").value("Bej Blazer"));
        mvc.perform(auth(get("/api/garments"), token).header("Accept-Language", "en").param("q", "blazer"))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(auth(get("/api/outfits/suggestions"), token).header("Accept-Language", "en"))
                .andExpect(jsonPath("$.readiness.missing[0].message").value("Add at least one top (or a dress) so we can suggest outfits."));
    }

    @Test
    void firebaseSignInIsUnavailableWithoutServerCredentials() throws Exception {
        mvc.perform(post("/api/auth/firebase").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"x\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("AUTH_UNAVAILABLE"));
        mvc.perform(get("/api/config"))
                .andExpect(jsonPath("$.auth.firebase").value(false))
                .andExpect(jsonPath("$.auth.local").value(true));
    }

    @Test
    void rejectsNonImageUploads() throws Exception {
        String token = register("zeynep@example.com", "Zeynep");
        MockMultipartFile file = new MockMultipartFile("file", "x.txt", "text/plain", "hello".getBytes());
        mvc.perform(auth(multipart("/api/images").file(file), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_IMAGE"));
    }

    private String register(String email, String name) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"displayName\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.onboardingCompleted").value(false))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String upload(String token, String background, String garment) throws Exception {
        byte[] jpeg = processor.toJpeg(SyntheticPhotos.garmentOn(background, garment, 600, 800, 3), 0.9f);
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpeg);
        return mvc.perform(auth(multipart("/api/images").file(file), token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private long createGarment(String token, String imageId, String category, String sub, String color, String name)
            throws Exception {
        String body = mvc.perform(auth(post("/api/garments"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(garmentJson(imageId, category, sub, color, name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static String garmentJson(String imageId, String category, String sub, String color, String name) {
        return "{\"imageId\":\"" + imageId + "\",\"category\":\"" + category + "\",\"subcategory\":\"" + sub
                + "\",\"color\":\"" + color + "\"" + (name == null ? "" : ",\"name\":\"" + name + "\"") + "}";
    }

    private static <B extends AbstractMockHttpServletRequestBuilder<B>> B auth(B builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }
}
