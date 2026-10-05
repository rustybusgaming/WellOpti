package dev.wellopti.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Keeps every translation in step with English: no stray keys, and the same %s / %% placeholders. */
class TranslationsTest {
	private static final Path LANG = Path.of("src/main/resources/assets/wellopti/lang");
	private static final Pattern PLACEHOLDER = Pattern.compile("%(?:\\d+\\$)?s|%%");

	/** Minecraft shows the raw key instead of the text if a string has a % that isn't a valid placeholder. */
	@Test
	void noStrayPercentSigns() throws IOException {
		try (Stream<Path> files = Files.list(LANG)) {
			for (Path file : files.toList()) {
				for (Map.Entry<String, String> entry : read(file).entrySet()) {
					String leftover = PLACEHOLDER.matcher(entry.getValue()).replaceAll("");
					assertTrue(leftover.indexOf('%') < 0, file.getFileName() + " has a stray % in " + entry.getKey() + " (write %% for a literal %)");
				}
			}
		}
	}

	@Test
	void translationsMatchEnglish() throws IOException {
		Map<String, String> english = read(LANG.resolve("en_us.json"));
		List<Path> others;
		try (Stream<Path> files = Files.list(LANG)) {
			others = files.filter(p -> !p.getFileName().toString().equals("en_us.json")).toList();
		}
		assertTrue(others.size() >= 4, "expected at least four translations");

		for (Path file : others) {
			Map<String, String> translated = read(file);
			for (Map.Entry<String, String> entry : translated.entrySet()) {
				String key = entry.getKey();
				assertTrue(english.containsKey(key), file.getFileName() + " has a key English doesn't: " + key);
				assertEquals(placeholders(english.get(key)), placeholders(entry.getValue()),
					file.getFileName() + " changes the placeholders in " + key);
			}
		}
	}

	private static Map<String, String> read(Path file) throws IOException {
		try (Reader reader = Files.newBufferedReader(file)) {
			return new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
		}
	}

	/** How many %s placeholders and %% literals a string has. Their order may differ between languages. */
	private static String placeholders(String text) {
		int args = 0;
		int literals = 0;
		Matcher matcher = PLACEHOLDER.matcher(text);
		while (matcher.find()) {
			if (matcher.group().equals("%%")) {
				literals++;
			} else {
				args++;
			}
		}
		return args + " argument(s), " + literals + " literal %";
	}
}
