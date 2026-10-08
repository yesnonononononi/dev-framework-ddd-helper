package com.summit.ddd.infrastructure.repository.yaml;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.summit.ddd.infrastructure.repository.AbstractRepository;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Implements the repository contract for a YAML list with stable {@link Long} identifiers.
 *
 * <p>Updates replace complete records, including null fields. Subclasses own model conversion
 * and timestamp policies; SQL-wrapper queries inherited from {@link AbstractRepository} are unsupported.</p>
 *
 * @param <M> domain model type
 * @param <P> serialized record type
 */
public abstract class AbstractYamlRepository<M, P> extends AbstractRepository<M, P, Long> {
    /** Custom queries must use this store so they share its locking and transaction state. */
    protected final YamlListStore<P> store;

    /**
     * @param store storage shared by CRUD operations and custom queries
     */
    protected AbstractYamlRepository(YamlListStore<P> store) {
        this.store = store;
    }

    /** Existing records must return a non-null identifier that survives serialization and updates. */
    protected abstract Long resolveId(P po);

    /** Returns the record to persist with the supplied identifier and any creation defaults. */
    protected abstract P prepareInsert(P po, long id);

    /**
     * Preserves immutable metadata while allowing replacement fields to be cleared.
     * Do not mutate {@code previous}; staged lists share their record objects.
     */
    protected abstract P prepareUpdate(P previous, P replacement);

    @Override
    protected @NotNull BaseMapper<P> mapper() {
        throw new UnsupportedOperationException("SQL query conditions are not supported by YAML repositories");
    }

    @Override
    public void save(M entity) {
        save(entity, null);
    }

    /**
     * Retains an explicit identifier or generates one above the largest stored identifier.
     *
     * @param entity model to insert; null is ignored
     * @param acquireId optional extractor for the prepared record's identifier
     * @return extracted identifier, or null when no extractor or entity is supplied
     * @throws IllegalStateException if the identifier already exists
     */
    @Override
    public Number save(M entity, Function<P, Number> acquireId) {
        if (entity == null) return null;
        return store.access(true, records -> {
            P po = toPO(entity);

            Long existingId = resolveId(po);

            long id = existingId == null ? Math.max(
                    IdWorker.getId(),
                    Math.addExact(records.stream()
                            .map(this::resolveId)
                            .filter(Objects::nonNull)
                            .max(Long::compareTo)
                            .orElse(0L), 1L))
                    : existingId;

            if (records.stream().anyMatch(record -> Objects.equals(resolveId(record), id))) {
                throw new IllegalStateException("Duplicate YAML record ID: " + id);
            }
            P saved = prepareInsert(po, id);

            records.add(saved);

            return acquireId == null ? null : acquireId.apply(saved);
        });
    }

    @Override
    public Optional<M> findById(@NonNull Long id) {
        return store.access(false, records -> records.stream()
                .filter(po -> Objects.equals(resolveId(po), id)).findFirst().map(this::toModel));
    }

    @Override
    public Collection<M> findList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return store.access(false, records -> records.stream().filter(po -> ids.contains(resolveId(po)))
                .sorted(Comparator.comparing(this::resolveId)).map(this::toModel).toList());
    }

    @Override
    public void delete(M entity) {
        if (entity != null) store.access(true, records -> records.removeIf(po ->
                Objects.equals(resolveId(po), resolveId(toPO(entity)))));
    }

    @Override
    public void updateById(@NotNull M entity) {
        update(List.of(entity));
    }

    /**
     * Replaces the batch only after every record has been prepared successfully.
     * Missing identifiers fail the operation rather than silently creating records.
     */
    @Override
    public void update(Collection<M> entities) {
        store.access(true, records -> {
            List<P> replacements = entities.stream().filter(Objects::nonNull).map(this::toPO).toList();
            for (P replacement : replacements) {
                int index = -1;
                for (int i = 0; i < records.size(); i++) {
                    if (Objects.equals(resolveId(records.get(i)), resolveId(replacement))) {
                        index = i;
                        break;
                    }
                }
                if (index < 0) throw new IllegalStateException("YAML record does not exist: " + resolveId(replacement));
                records.set(index, prepareUpdate(records.get(index), replacement));
            }
            return null;
        });
    }

    /** Returns an identifier-ordered page; page number and size are clamped to at least one. */
    @Override
    public IPage<M> queryByPage(int current, int size) {
        return store.access(false, records -> {
            Page<M> page = new Page<>(Math.max(current, 1), Math.max(size, 1), records.size());
            long offset = (page.getCurrent() - 1) * page.getSize();
            page.setRecords(records.stream().sorted(Comparator.comparing(this::resolveId))
                    .skip(offset).limit(page.getSize()).map(this::toModel).toList());
            return page;
        });
    }
}
