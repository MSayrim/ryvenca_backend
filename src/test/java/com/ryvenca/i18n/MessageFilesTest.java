package com.ryvenca.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.core.io.ClassPathResource;

/** Every language file must translate every key and keep the same placeholders. */
class MessageFilesTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\d+}");

    @ParameterizedTest
    @EnumSource(Language.class)
    void languageFileIsCompleteAndConsistent(Language language) throws IOException {
        Properties base = load("i18n/messages.properties");
        String file = language == Language.EN ? "i18n/messages.properties" : "i18n/messages_" + language.code() + ".properties";
        assertThat(new ClassPathResource(file).exists()).as("%s exists", file).isTrue();
        Properties translated = load(file);
        assertThat(new TreeSet<>(translated.stringPropertyNames())).as("keys of %s", file)
                .isEqualTo(new TreeSet<>(base.stringPropertyNames()));
        for (String key : base.stringPropertyNames()) {
            String value = translated.getProperty(key);
            assertThat(value).as("%s in %s", key, file).isNotBlank();
            if (!key.startsWith("grammar.") && !key.equals("garment.generatedName")) {
                assertThat(placeholders(value)).as("placeholders of %s in %s", key, file)
                        .isEqualTo(placeholders(base.getProperty(key)));
            }
        }
        assertThat(translated.getProperty("grammar.lowercase")).isIn("true", "false");
        for (String key : new String[] {"grammar.phrase", "grammar.list.middle", "grammar.list.last", "grammar.sentences"}) {
            assertThat(placeholders(translated.getProperty(key))).as(key).containsExactlyInAnyOrder("{0}", "{1}");
        }
    }

    private static Set<String> placeholders(String value) {
        Set<String> found = new TreeSet<>();
        Matcher m = PLACEHOLDER.matcher(value);
        while (m.find()) {
            found.add(m.group());
        }
        return found;
    }

    private static Properties load(String path) throws IOException {
        Properties p = new Properties();
        try (Reader r = new InputStreamReader(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return p;
    }
}
