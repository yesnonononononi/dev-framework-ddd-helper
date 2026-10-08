package com.summit.ddd.infrastructure.conf;

import com.summit.ddd.infrastructure.event.DomainEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class DomainConfig {
    @Bean
    public DomainEventPublisher domainEventPublisher(ApplicationEventPublisher applicationEventPublisher){
        return new DomainEventPublisher(applicationEventPublisher);
    }
}
