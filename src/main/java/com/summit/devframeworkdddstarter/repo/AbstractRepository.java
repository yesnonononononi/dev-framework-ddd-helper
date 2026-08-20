package com.summit.devframeworkdddstarter.repo;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;


public abstract class AbstractRepository<M, P> implements RepositoryTemplate<M, P> {


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
    public Optional<M> findById(Long id) {
        if (id == null)
            return Optional.empty();
        P p = mapper().selectById(id);
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

    protected Collection<M> findList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty())
            return List.of();
        return mapper().selectByIds(ids).stream()
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
    public void updateById(M entity) {
        P po = this.toPO(entity);
        mapper().updateById(po);
    }

    @Override
    public void update(Collection<M> list) {
        List<P> l = list.stream().filter(Objects::nonNull).map(this::toPO).toList();
        mapper().updateById(l);
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
