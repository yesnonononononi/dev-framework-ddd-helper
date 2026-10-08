package com.summit.ddd.infrastructure.repository.yaml;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Coordinates list reads, migration, and writes through a shared fair JVM lock.
 *
 * <p>Spring transactions retain the lock until completion and publish staged changes after commit.
 * Rollback discards changes. Other processes are not locked, and file writes cannot participate
 * in an atomic commit with a database or other files.</p>
 *
 * @param <P> serialized record type
 */
public final class YamlListStore<P> {
    // A shared lock avoids lock-order inversion when a transaction accesses multiple files.
    private static final ReentrantLock CONFIG_LOCK = new ReentrantLock(true);
    private final Path path;
    private final YamlListCodec<P> codec;
    private final Supplier<List<P>> legacy;
    private final ReentrantLock lock;

    /**
     * @param path file path, normalized to identify transaction-local state
     * @param codec serializer responsible for safe file replacement
     * @param legacy records to import only when the file is absent; rollback may invoke it again
     */
    public YamlListStore(Path path, YamlListCodec<P> codec, Supplier<List<P>> legacy) {
        this.path = path.toAbsolutePath().normalize();
        this.codec = codec;
        this.legacy = legacy;
        this.lock = CONFIG_LOCK;
    }

    /** Creates missing files from an empty list rather than importing existing data. */
    public YamlListStore(Path path, YamlListCodec<P> codec) {
        this(path, codec, List::of);
    }

    /**
     * Runs an operation while holding the shared lock. Without a Spring transaction, successful
     * writes are persisted before returning; inside one, they remain staged until commit.
     *
     * <p>Read operations must not mutate the list or its records. Write operations must replace
     * records rather than mutate existing objects because transaction copies are shallow.
     * Operations must not retain the supplied list. A read that imports a missing file also schedules a write.</p>
     *
     * @param modifying whether a successful operation replaces the stored list
     * @param operation callback using the current list or transaction-local working copy
     * @param <R> callback result type
     * @return callback result
     * @throws IllegalStateException when a write is requested in a read-only transaction
     */
    public <R> R access(boolean modifying, Function<List<P>, R> operation) {

        boolean transactional = TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive();

        if (modifying && transactional && TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            throw new IllegalStateException("Cannot modify YAML records in a read-only transaction");
        }
        lock.lock();
        boolean retained = false;
        try {
            if (transactional) {

                @SuppressWarnings("unchecked")
                Pending<P> pending = (Pending<P>) TransactionSynchronizationManager.getResource(path);

                if (pending == null) {

                    pending = load();

                    Pending<P> state = pending;

                    TransactionSynchronizationManager.bindResource(path, state);

                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void suspend() {
                            TransactionSynchronizationManager.unbindResource(path);
                        }
                        @Override
                        public void resume() { TransactionSynchronizationManager.bindResource(path, state); }
                        @Override
                        public void afterCommit() {
                            if (state.dirty) codec.write(path, state.records);
                        }
                        @Override
                        public void afterCompletion(int status) {
                            TransactionSynchronizationManager.unbindResource(path);
                            lock.unlock();
                        }
                    });
                    retained = true;
                }
                List<P> working = new ArrayList<>(pending.records);
                R result = operation.apply(working);
                if (modifying) {
                    pending.records.clear();
                    pending.records.addAll(working);
                }
                pending.dirty |= modifying;
                return result;
            }
            Pending<P> pending = load();
            R result = operation.apply(pending.records);
            if (modifying || pending.dirty) codec.write(path, pending.records);
            return result;
        } finally {
            if (!retained) lock.unlock();
        }
    }

    private Pending<P> load() {
        boolean migrate = !Files.exists(path);

        List<P> records = migrate ? legacy.get() : codec.read(path);

        if (!migrate && records == null) throw new IllegalStateException("YAML records must not be empty: " + path);

        return new Pending<>(new ArrayList<>(records == null ? List.of() : records), migrate);
    }

    private static final class Pending<P> {
        private final List<P> records;
        private boolean dirty;
        private Pending(List<P> records, boolean dirty) { this.records = records; this.dirty = dirty; }
    }
}
