package com.summit.ddd.infrastructure.repository.json;

import com.summit.ddd.infrastructure.repository.file.AbstractFileRepository;
import com.summit.ddd.infrastructure.repository.file.FileListStore;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/** JSON repository with shared file locking, transaction staging, and stable Long identifiers. */
public abstract class AbstractJsonRepository<M, P> extends AbstractFileRepository<M, P> {
    /** Creates missing files from an empty list. */
    protected AbstractJsonRepository(Path path, Class<P> recordType) {
        super(new FileListStore<>(path, new JsonListCodec<>(recordType)));
    }

    /** Imports legacy records only when the JSON file is absent; rollback discards the import. */
    protected AbstractJsonRepository(Path path, Class<P> recordType, Supplier<List<P>> legacy) {
        super(new FileListStore<>(path, new JsonListCodec<>(recordType), legacy));
    }
}
