package com.academicpassport.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap.super-admin")
public record SuperAdminBootstrapProperties(
    boolean enabled,
    String email,
    String password
) {}
