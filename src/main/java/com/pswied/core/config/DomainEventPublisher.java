package com.pswied.core.config;

public interface DomainEventPublisher {

    void publish(Object event);
}
