package com.summit.ddd.infrastructure.repository.yaml;

import java.io.IOException;
import java.nio.file.Path;

/** Retains the YAML atomic writer API for existing codecs. */
public final class AtomicFileWriter {
    private AtomicFileWriter() {}

    /**
     * Delegates to the shared atomic writer without a non-atomic fallback.
     * @param path destination file
     * @param writer callback that closes all output before returning
     * @throws IOException if serialization, replacement, or cleanup fails
     */
    public static void write(Path path, ContentWriter writer) throws IOException {
        com.summit.ddd.infrastructure.repository.file.AtomicFileWriter.write(path, writer::write);
    }

    /** Supplies content without owning destination replacement or cleanup. */
    @FunctionalInterface
    public interface ContentWriter {
        /**
         * @param temporary existing temporary file to populate
         * @throws IOException if content cannot be written completely
         */
        void write(Path temporary) throws IOException;
    }
}
