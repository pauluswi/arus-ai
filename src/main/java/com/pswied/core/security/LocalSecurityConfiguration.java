package com.pswied.core.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
@Profile({"local", "test"})
public class LocalSecurityConfiguration {

    @Bean
    UserDetailsService disabledPasswordAuthentication() {
        return username -> {
            throw new UsernameNotFoundException("Password authentication is disabled in the local mock profile.");
        };
    }

    @Bean
    FilterRegistrationBean<MockIdentityFilter> disableMockFilterServletRegistration(
            ObjectProvider<MockIdentityFilter> mockIdentityFilter) {
        FilterRegistrationBean<MockIdentityFilter> registration = new FilterRegistrationBean<>();
        MockIdentityFilter filter = mockIdentityFilter.getIfAvailable();
        if (filter != null) {
            registration.setFilter(filter);
        }
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    SecurityFilterChain localSecurityFilterChain(
            HttpSecurity http,
            ObjectProvider<MockIdentityFilter> mockIdentityFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        MockIdentityFilter filter = mockIdentityFilter.getIfAvailable();
        if (filter != null) {
            http.addFilterBefore(filter, AnonymousAuthenticationFilter.class);
        }
        return http.build();
    }
}
