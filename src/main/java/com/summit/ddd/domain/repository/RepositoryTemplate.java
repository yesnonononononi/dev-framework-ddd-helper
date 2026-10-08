package com.summit.ddd.domain.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.Collection;
import java.util.Optional;

/**
 * 领域层（Domain）仓储接口
 * <p>仓储抽象定义在领域层，实现下沉到基础设施层 {@code infrastructure}。</p>
 *
 * @param <M> 领域模型（Model）
 * @param <ID> 标识类型
 */
public interface RepositoryTemplate<M, ID> {

    void save(M entity);

    void delete(M entity);

    Optional<M> findById(ID id);

    void updateById(M entity);

    void update(Collection<M> list);

    IPage<M> queryByPage(int current, int size);
}
