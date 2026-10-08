package com.summit.ddd.infrastructure.repository.yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Serializes to a sibling temporary file before atomically replacing the destination.
 * This prevents partial content from replacing a valid file; it does not provide locking
 * or guarantee durability after a power failure.
 */
public final class AtomicFileWriter {
    private AtomicFileWriter() {}
    private static final String TEMPORARY_PREFIX = ".ddd-";
    private static final String TEMPORARY_SUFFIX = ".tmp";


    /**
     * Creates parent directories and removes the temporary file on success or failure.
     * No non-atomic fallback is used when the filesystem cannot support atomic replacement.
     *
     * @param path destination file
     * @param writer callback that completes and closes all output before returning
     * @throws IOException if serialization, replacement, or cleanup fails; cleanup errors are
     *         suppressed on an earlier failure so its cause is preserved
     */
    public static void write(Path path, ContentWriter writer) throws IOException {
        Path target = path.toAbsolutePath().normalize();

        // A sibling temporary file keeps the rename on the same filesystem.
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), TEMPORARY_PREFIX, TEMPORARY_SUFFIX);

        Throwable failure = null;
        try {
            writer.write(temporary);

            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException | Error e) {
            failure = e;
            throw e;
        } finally {

            try { Files.deleteIfExists(temporary); }
            catch (IOException e) {
                if (failure != null) failure.addSuppressed(e);
                else throw e;
            }
        }
    }

    /** Supplies file content without owning destination replacement or temporary-file cleanup. */
    @FunctionalInterface
    public interface ContentWriter {
        /**
         * @param temporary existing temporary file to populate; streams must be closed before returning
         * @throws IOException if the content cannot be written completely
         */
        void write(Path temporary) throws IOException;
    }
}
