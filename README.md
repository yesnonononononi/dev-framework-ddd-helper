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

## JSON repositories

Version `1.0.6-SNAPSHOT` adds `AbstractJsonRepository<M, P>` and `JsonListCodec<P>`
under `com.summit.ddd.infrastructure.repository.json`.
Pass `Path` and the PO class to the repository constructor. An optional third
`Supplier<List<P>>` imports database records only when the JSON file is absent.
No YAML migration or format fallback is performed.

The codec requires a JSON array of non-null records, rejects malformed input and trailing
values, preserves Long numbers and ISO timestamps, and uses its own Jackson mapper.
Implement the same conversion, identifier, and metadata hooks as the YAML repository.
Generic codecs use `FileListStore<P>` and `AbstractFileRepository<M, P>` from
`com.summit.ddd.infrastructure.repository.file`. YAML and JSON stores share one JVM lock
and transaction state. Rollback discards staged writes; commit publishes files atomically.
Multi-file/database atomicity and cross-process locking are not provided.
