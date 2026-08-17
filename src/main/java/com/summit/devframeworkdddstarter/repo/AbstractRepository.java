package com.summit.devframeworkdddstarter.repo;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;


public abstract class AbstractRepository<M, P> implements RepositoryTemplate<M, P> {
    public AbstractRepository(BaseMapper<P> baseMapper) {
        this.baseMapper = baseMapper;
    }

    private final BaseMapper<P> baseMapper;

    protected BaseMapper<P> getBaseMapper() {
        return baseMapper;
    }

    @Override
    public void save(M entity) {
        this.save(entity, null);
    }

    @Override
    public void delete(M entity) {
        if (entity == null)
            return;
        baseMapper.deleteById(this.toPO(entity));
    }

    @Override
    public Optional<M> findById(Long id) {
        if (id == null)
            return Optional.empty();
        P p = baseMapper.selectById(id);
        if (Objects.isNull(p))
            return Optional.empty();
        return Optional.of(this.toModel(p));
    }

    protected Optional<M> findBy(Object val, SFunction<P, ?> by) {
        if (val == null)
            return Optional.empty();
        LambdaQueryWrapper<P> wrapper = new LambdaQueryWrapper<P>().eq(by, val);
        P p = baseMapper.selectOne(wrapper);
        if (Objects.isNull(p))
            return Optional.empty();
        return Optional.of(this.toModel(p));
    }

    protected Collection<M> findList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty())
            return List.of();
        return baseMapper.selectByIds(ids).stream()
                .map(this::toModel)
                .toList();
    }

    protected List<M> findListBy(Object val, SFunction<P, ?> by) {
        if (val == null)
            return List.of();
        LambdaQueryWrapper<P> wrapper = new LambdaQueryWrapper<P>().eq(by, val);
        return baseMapper.selectList(wrapper).stream()
                .map(this::toModel)
                .toList();
    }

    protected List<M> findListIn(Collection<?> vals, SFunction<P, ?> by) {
        if (vals == null || vals.isEmpty())
            return List.of();
        LambdaQueryWrapper<P> wrapper = new LambdaQueryWrapper<P>().in(by, vals);
        return baseMapper.selectList(wrapper).stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public void updateById(M entity) {
        P po = this.toPO(entity);
        baseMapper.updateById(po);
    }

    @Override
    public void update(Collection<M> list) {
        List<P> l = list.stream().filter(Objects::nonNull).map(this::toPO).toList();
        baseMapper.updateById(l);
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
        baseMapper.insert(po);
        return acquireId == null ? null : acquireId.apply(po);
    }

    protected void delete(Object val, SFunction<P, ?> by) {
        if (val == null)
            return;
        LambdaQueryWrapper<P> queryWrapper = new LambdaQueryWrapper<P>().eq(by, val);
        baseMapper.delete(queryWrapper);
    }

    protected abstract P toPO(M entity);

    protected abstract M toModel(P po);
}
