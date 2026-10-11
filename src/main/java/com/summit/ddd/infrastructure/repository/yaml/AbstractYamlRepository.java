package com.summit.ddd.infrastructure.repository.yaml;

import com.summit.ddd.infrastructure.repository.file.AbstractFileRepository;

/**
 * Retains the YAML repository API while using the shared file CRUD implementation.
 *
 * @param <M> domain model type
 * @param <P> serialized record type
 */
public abstract class AbstractYamlRepository<M, P> extends AbstractFileRepository<M, P> {
    /** Keeps existing custom YAML queries on the same store as inherited CRUD operations. */
    protected final YamlListStore<P> store;

    /**
     * Uses the same lock and transaction state as JSON repositories.
     * @param store storage shared by CRUD operations and custom queries
     */
    protected AbstractYamlRepository(YamlListStore<P> store) {
        super(store.unwrap());
        this.store = store;
    }
}
