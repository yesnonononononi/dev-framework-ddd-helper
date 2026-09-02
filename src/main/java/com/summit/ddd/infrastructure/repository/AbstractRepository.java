package com.summit.ddd.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.summit.ddd.domain.model.AggregateRoot;
import com.summit.ddd.domain.repository.RepositoryTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * 基础设施层（Infrastructure）仓储实现模板
 * <p>基于 MyBatis-Plus 的仓储实现，屏蔽持久化细节，仅领域层接口 {@link RepositoryTemplate} 对上层可见。</p>
 *
 * @param <M> 领域模型（Model）
 * @param <P> 持久化对象（PO）
 */
@SuppressWarnings("unchecked")
public abstract class AbstractRepository<M extends AggregateRoot<ID>, P,ID> implements RepositoryTemplate<M,ID> {

    @Override
    public void save(M entity) {
        this.save(entity, null);
    }

    @Override
    public void delete(M entity) {
        if (entity == null)
            return;
        mapper().deleteById(this.toPO(entity));
    }

    @Override
    public Optional<M> findById(@NotNull ID id) {
        P p = mapper().selectById((Serializable) id);
        if (Objects.isNull(p))
            return Optional.empty();
        return Optional.of(this.toModel(p));
    }

    protected Optional<M> findBy(Object val, SFunction<P, ?> by) {
        if (val == null)
            return Optional.empty();
        LambdaQueryWrapper<P> wrapper = new LambdaQueryWrapper<P>().eq(by, val);
        P p = mapper().selectOne(wrapper);
        if (Objects.isNull(p))
            return Optional.empty();
        return Optional.of(this.toModel(p));
    }

    protected Collection<M> findList(Collection<ID> ids) {
        if (ids == null || ids.isEmpty())
            return List.of();
        return mapper().selectByIds((Collection<? extends Serializable>) ids).stream()
                .map(this::toModel)
                .toList();
    }

    protected List<M> findListBy(Object val, SFunction<P, ?> by) {
        if (val == null)
            return List.of();
        LambdaQueryWrapper<P> wrapper = new LambdaQueryWrapper<P>().eq(by, val);
        return mapper().selectList(wrapper).stream()
                .map(this::toModel)
                .toList();
    }

    protected List<M> findListIn(Collection<?> vals, SFunction<P, ?> by) {
        if (vals == null || vals.isEmpty())
            return List.of();
        LambdaQueryWrapper<P> wrapper = new LambdaQueryWrapper<P>().in(by, vals);
        return mapper().selectList(wrapper).stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public void updateById(@NotNull M entity) {
        P po = this.toPO(entity);
        mapper().updateById(po);
    }

    @Override
    public void update(Collection<M> list) {
        List<P> l = list.stream().filter(Objects::nonNull).map(this::toPO).toList();
        mapper().updateById(l);
    }

    @Override
    public IPage<M> queryByPage(int current, int size) {
        return queryByPage(current, size, null);
    }

    public IPage<M> queryByPage(int current, int size, @Nullable QueryWrapper<P> queryWrapper) {
        return mapper().selectPage(new Page<>(Math.max(current, 1), Math.max(size, 1)), Objects.requireNonNullElse(queryWrapper, new QueryWrapper<>()))
                .convert(this::toModel);
    }

    /**
     * 保存实体，返回实体ID
     *
     * @param entity 实体
     * @return 实体ID
     */
    public Number save(M entity, Function<P, Number> acquireId) {
        if (entity == null)
            return null;
        P po = this.toPO(entity);
        mapper().insert(po);
        return acquireId == null ? null : acquireId.apply(po);
    }

    protected void delete(Object val, SFunction<P, ?> by) {
        if (val == null)
            return;
        LambdaQueryWrapper<P> queryWrapper = new LambdaQueryWrapper<P>().eq(by, val);
        mapper().delete(queryWrapper);
    }

    protected abstract P toPO(M entity);

    protected abstract M toModel(P po);

    protected abstract @NotNull BaseMapper<P> mapper();
}
