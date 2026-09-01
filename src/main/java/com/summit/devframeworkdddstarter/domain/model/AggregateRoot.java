package com.summit.devframeworkdddstarter.domain.model;

import com.summit.devframeworkdddstarter.domain.event.DomainEvent;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 领域层（Domain）聚合根基类
 * <p>承载聚合标识、乐观锁版本号与领域事件收集。子类通过 {@link #registerEvent} 记录领域事件，
 * 应用层在保存聚合后调用 {@link #pull} 取出并清空事件，交由基础设施层在事务提交后发布。</p>
 *
 * @param <ID> 聚合标识类型
 */
@Getter
public abstract class AggregateRoot<ID> {
    private final ID id;
    private long version;
    @Getter(AccessLevel.NONE)
    private final List<DomainEvent> events = new ArrayList<>();

    protected AggregateRoot(ID id) {
        this.id = id;
    }

    protected AggregateRoot() {
        this.id = null;
    }

    protected void registerEvent(@NotNull DomainEvent event) {
        this.events.add(event);
    }

    public Collection<DomainEvent> pull() {
        if (this.events.isEmpty())
            return List.of();
        List<DomainEvent> snapshot = List.copyOf(this.events);
        this.events.clear();
        return snapshot;
    }
}
