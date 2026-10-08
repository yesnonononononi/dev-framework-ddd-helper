## 引入
```xml
<dependency>
    <groupId>io.github.yesnonononononi</groupId>
    <artifactId>dev-framework-ddd-helper</artifactId>
    <version>1.0.4</version>
</dependency>
```

## YAML repositories

Version `1.0.5-SNAPSHOT` provides `AbstractYamlRepository<M, P>` and `YamlListStore<P>`
under `com.summit.ddd.infrastructure.repository.yaml`.
Repository interfaces continue to extend `RepositoryTemplate<M, Long>`.

Implement `YamlListCodec<P>` with the serialization library used by your application.
Use `AtomicFileWriter.write(path, writer)` in the codec to serialize to a temporary file
and atomically replace the destination. YAML libraries are not added transitively.

Construct the store with `new YamlListStore<>(path, codec)` for an empty initial list,
or pass a third `Supplier<List<P>>` argument for one-time migration when the file is absent.
A present file must decode to a list; return an empty list for an empty collection.
Implement `toPO`, `toModel`, `resolveId`, `prepareInsert`, and `prepareUpdate` in the repository.
Custom filters can use `store.access(false, records -> ...)`. Read operations must not mutate records;
write operations must replace records rather than mutate previously loaded objects.

The shared fair JVM lock serializes read-modify-write operations and remains held until
Spring transaction completion. Writes are staged in the transaction and published after commit;
rollback discards them. Cross-process locking and atomic commits spanning files and databases
are not provided. SQL-wrapper query methods are unsupported by YAML repositories.
