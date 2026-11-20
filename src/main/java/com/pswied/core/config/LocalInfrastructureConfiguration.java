package com.pswied.core.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pswied.core.network.NetworkSubmissionStatusView;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.UUID;

@Configuration
@Profile({"local", "test"})
public class LocalInfrastructureConfiguration {

    @Bean
    Cache<UUID, NetworkSubmissionStatusView> networkSubmissionCache() {
        return Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .build();
    }
}
