package com.roomfit.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GuestAuthProperties.class)
public class GuestAuthConfig {
}
