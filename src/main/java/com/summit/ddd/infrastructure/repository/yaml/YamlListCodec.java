package com.summit.ddd.infrastructure.repository.yaml;

import lombok.NonNull;

import java.nio.file.Path;
import java.util.List;

/**
 * Keeps serialization libraries and record schemas outside the repository infrastructure.
 * Implementations must report malformed data instead of silently substituting default records.
 *
 * @param <P> serialized record type
 */
public interface YamlListCodec<P> {
    /**
     * Reads an existing file. Return an empty list for an empty collection, never null.
     * Missing-file handling belongs to {@link YamlListStore}.
     */
    @NonNull List<P> read(Path path);
    /**
     * Persists the complete list without modifying it. Use {@link AtomicFileWriter} to prevent
     * a serialization failure from truncating the existing file; propagate write failures.
     */
    void write(Path path, List<P> records);
}
