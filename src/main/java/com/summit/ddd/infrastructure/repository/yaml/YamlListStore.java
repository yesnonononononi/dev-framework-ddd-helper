package com.summit.ddd.infrastructure.repository.yaml;

import com.summit.ddd.infrastructure.repository.file.FileListStore;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/** Retains the YAML API while sharing locks and transaction state with all file repositories. */
public final class YamlListStore<P> {
    private final FileListStore<P> delegate;

    public YamlListStore(Path path, YamlListCodec<P> codec, Supplier<List<P>> legacy) {
        delegate = new FileListStore<>(path, codec, legacy);
    }

    public YamlListStore(Path path, YamlListCodec<P> codec) {
        this(path, codec, List::of);
    }

    FileListStore<P> unwrap() { return delegate; }

    public <R> R access(boolean modifying, Function<List<P>, R> operation) {
        return delegate.access(modifying, operation);
    }
}
