package com.summit.ddd.infrastructure.event;

import com.summit.ddd.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;

/**
 * 基础设施层（Infrastructure）领域事件发布器
 * <p>在事务提交后发布领域事件：存在活跃事务时注册事务同步回调、提交成功后发布，
 * 确保事件不会因后续回滚而对外泄漏；无事务时立即发布。</p>
 */
@Component
@RequiredArgsConstructor
public class DomainEventPublisher {
    private final ApplicationEventPublisher publisher;

    public void publishAfterCommit(Collection<DomainEvent> events) {
        if (events == null || events.isEmpty())
            return;
        List<DomainEvent> snapshot = List.copyOf(events);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    snapshot.forEach(publisher::publishEvent);
                }
            });
        } else {
            snapshot.forEach(publisher::publishEvent);
        }
    }
}
