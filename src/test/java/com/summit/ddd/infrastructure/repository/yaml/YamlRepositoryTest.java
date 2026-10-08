package com.summit.ddd.infrastructure.repository.yaml;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class YamlRepositoryTest {
    @TempDir Path directory;

    private record Entry(Long id, String name) {}

    // A minimal codec verifies that storage has no dependency on a serialization library.
    private final YamlListCodec<Entry> codec = new YamlListCodec<>() {
        @Override
        public @NonNull List<Entry> read(Path path) {
            try {
                return Files.readAllLines(path).stream().map(line -> {
                    String[] fields = line.split(": ", 2);
                    return new Entry(Long.valueOf(fields[0]), fields[1]);
                }).toList();
            } catch (IOException e) { throw new IllegalStateException(e); }
        }
        @Override
        public void write(Path path, List<Entry> records) {
            try {
                AtomicFileWriter.write(path, temporary -> Files.write(temporary,
                        records.stream().map(entry -> entry.id() + ": " + entry.name()).toList()));
            } catch (IOException e) { throw new IllegalStateException(e); }
        }
    };

    private static final class Entries extends AbstractYamlRepository<Entry, Entry> {
        private Entries(YamlListStore<Entry> store) { super(store); }
        @Override protected Entry toPO(Entry entry) { return entry; }
        @Override protected Entry toModel(Entry entry) { return entry; }
        @Override protected Long resolveId(Entry entry) { return entry.id(); }
        @Override protected Entry prepareInsert(Entry entry, long id) { return new Entry(id, entry.name()); }
        @Override protected Entry prepareUpdate(Entry previous, Entry replacement) { return replacement; }
    }

    private static final class Transactions extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {}
        @Override protected void doCommit(DefaultTransactionStatus status) {}
        @Override protected void doRollback(DefaultTransactionStatus status) {}
    }

    @Test
    void crudAndReopeningKeepStableIds() {
        Path path = directory.resolve("entries.yaml");
        Entries entries = new Entries(new YamlListStore<>(path, codec));
        entries.save(new Entry(1L, "first"));
        Number generated = entries.save(new Entry(null, "second"), Entry::id);
        assertEquals(2, entries.queryByPage(1, 1).getTotal());
        assertEquals(1, entries.findList(List.of(1L)).size());
        entries.updateById(new Entry(1L, "updated"));
        assertEquals("updated", new Entries(new YamlListStore<>(path, codec)).findById(1L).orElseThrow().name());
        entries.delete(new Entry(1L, "updated"));
        assertTrue(entries.findById(1L).isEmpty());
        assertTrue(entries.findById(generated.longValue()).isPresent());
    }

    @Test
    void failedSerializationPreservesOriginalAndCleansTemporaryFile() throws Exception {
        Path path = directory.resolve("entries.yaml");
        Files.writeString(path, "original");
        assertThrows(IOException.class, () -> AtomicFileWriter.write(path, temporary -> {
            Files.writeString(temporary, "partial");
            throw new IOException("serialization failed");
        }));
        assertEquals("original", Files.readString(path));
        try (Stream<Path> files = Files.list(directory)) {
            assertEquals(List.of(path), files.toList());
        }
    }

    @Test
    void rollbackDiscardsMigrationAndCommitPublishesRecords() {
        Path path = directory.resolve("entries.yaml");
        Entries entries = new Entries(new YamlListStore<>(path, codec, () -> List.of(new Entry(4L, "migrated"))));
        TransactionTemplate transaction = new TransactionTemplate(new Transactions());
        transaction.executeWithoutResult(status -> {
            entries.updateById(new Entry(4L, "rolled-back"));
            assertEquals("rolled-back", entries.findById(4L).orElseThrow().name());
            assertFalse(Files.exists(path));
            status.setRollbackOnly();
        });
        assertFalse(Files.exists(path));
        transaction.executeWithoutResult(status -> entries.updateById(new Entry(4L, "committed")));
        assertEquals("committed", codec.read(path).getFirst().name());
        TransactionTemplate readOnly = new TransactionTemplate(new Transactions());
        readOnly.setReadOnly(true);
        assertThrows(IllegalStateException.class, () -> readOnly.executeWithoutResult(status -> entries.save(new Entry(5L, "denied"))));
        assertEquals(1, entries.queryByPage(1, 10).getTotal());
    }

    @Test
    void failedBatchDoesNotLeakPartialChangesIntoTransaction() {
        Path path = directory.resolve("entries.yaml");
        Entries entries = new Entries(new YamlListStore<>(path, codec));
        entries.save(new Entry(1L, "original"));
        new TransactionTemplate(new Transactions()).executeWithoutResult(status -> {
            assertThrows(IllegalStateException.class, () -> entries.update(List.of(new Entry(1L, "partial"), new Entry(99L, "missing"))));
            assertEquals("original", entries.findById(1L).orElseThrow().name());
        });
        assertEquals("original", codec.read(path).getFirst().name());
    }

    @Test
    void concurrentTransactionsKeepEveryRecord() throws Exception {
        Path path = directory.resolve("entries.yaml");
        Entries entries = new Entries(new YamlListStore<>(path, codec));
        try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
            List<Callable<Number>> tasks = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                tasks.add(() -> new TransactionTemplate(new Transactions()).execute(status -> entries.save(new Entry(null, "created"), Entry::id)));
            }
            List<Number> ids = new ArrayList<>();
            for (Future<Number> future : executor.invokeAll(tasks)) ids.add(future.get());
            assertEquals(20, ids.stream().distinct().count());
            assertEquals(20, codec.read(path).size());
        }
    }

    @Test
    void commitWriteFailureReleasesLockForAnotherThread() throws Exception {
        Path path = directory.resolve("entries.yaml");
        YamlListCodec<Entry> failing = new YamlListCodec<>() {
            @Override public @NonNull List<Entry> read(Path file) { return codec.read(file); }
            @Override public void write(Path file, List<Entry> records) {
                throw new IllegalStateException("simulated disk failure");
            }
        };
        Entries entries = new Entries(new YamlListStore<>(path, failing));
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(new Transactions())
                .executeWithoutResult(status -> entries.save(new Entry(1L, "failed"))));
        assertFalse(Files.exists(path));
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> new Entries(new YamlListStore<>(path, codec))
                    .save(new Entry(2L, "recovered"))).get(3, TimeUnit.SECONDS);
        }
        assertEquals(2L, codec.read(path).getFirst().id());
    }

}
