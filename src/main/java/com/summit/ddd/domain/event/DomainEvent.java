package com.summit.ddd.domain.event;

import java.time.Instant;

/**
 * 领域层（Domain）领域事件标记接口
 * <p>所有领域事件实现该接口，事件为不可变快照，建议使用 {@code record} 声明（record 组件
 * 自动生成的访问器即可满足 {@link #occurAt()}）。由聚合根 {@code registerEvent} 收集，
 * 应用层保存聚合后 {@code pull} 取出，经 {@code DomainEventPublisher} 在事务提交后发布。</p>
 */
public interface DomainEvent {

    /**
     * 事件发生时间
     */
    Instant occurAt();
}
