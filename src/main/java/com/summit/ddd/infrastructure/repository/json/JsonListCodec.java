package com.summit.ddd.infrastructure.repository.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.summit.ddd.infrastructure.repository.file.AtomicFileWriter;
import com.summit.ddd.infrastructure.repository.file.FileListCodec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Strict JSON arrays with numeric identifiers, ISO dates, and atomic file replacement. */
public final class JsonListCodec<P> implements FileListCodec<P> {
    private final ObjectMapper mapper;
    private final JavaType listType;

    /** Uses a private mapper so application HTTP serialization cannot change file contents. */
    public JsonListCodec(Class<P> recordType) {
        Objects.requireNonNull(recordType, "recordType");
        mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        listType = mapper.getTypeFactory().constructCollectionType(List.class, recordType);
    }

    @Override
    public List<P> read(Path path) {
        try {
            List<P> records = mapper.readValue(path.toFile(), listType);
            if (records == null || records.stream().anyMatch(Objects::isNull)) {
                throw new IllegalStateException("JSON configuration must be an array of non-null records: " + path);
            }
            return records;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read JSON configuration: " + path, e);
        }
    }

    @Override
    public void write(Path path, List<P> records) {
        Objects.requireNonNull(records, "records");
        if (records.stream().anyMatch(Objects::isNull)) {
            throw new IllegalStateException("JSON configuration must not contain null records: " + path);
        }
        try {
            AtomicFileWriter.write(path, temporary -> mapper.writeValue(temporary.toFile(), records));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save JSON configuration: " + path, e);
        }
    }
}
