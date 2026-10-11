package com.summit.ddd.infrastructure.repository.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonListCodecTest {
    @TempDir Path directory;
    private record Entry(Long id, String name, Instant created) {}

    @Test
    void roundTripUsesStrictJsonNumbersAndIsoDates() throws Exception {
        Path path = directory.resolve("nested/entries.json");
        JsonListCodec<Entry> codec = new JsonListCodec<>(Entry.class);
        Entry entry = new Entry(9007199254740993L, "中文\n\"引号\"", Instant.parse("2026-10-11T01:02:03Z"));
        codec.write(path, List.of(entry));
        assertEquals(List.of(entry), codec.read(path));
        ObjectMapper strict = new ObjectMapper();
        assertTrue(strict.readTree(Files.readString(path)).get(0).get("id").isIntegralNumber());
        assertEquals("2026-10-11T01:02:03Z", strict.readTree(Files.readString(path)).get(0).get("created").asText());
    }

    @Test
    void rejectsMalformedYamlNullRecordsAndTrailingValues() throws Exception {
        Path path = directory.resolve("entries.json");
        JsonListCodec<Entry> codec = new JsonListCodec<>(Entry.class);
        for (String invalid : List.of("- id: 1", "[", "", "null", "[null]", "{}", "[] {}")) {
            Files.writeString(path, invalid);
            assertThrows(IllegalStateException.class, () -> codec.read(path), invalid);
            assertEquals(invalid, Files.readString(path));
        }
        Files.writeString(path, "[]");
        assertTrue(codec.read(path).isEmpty());
    }
}
