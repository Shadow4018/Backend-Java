package com.java.tms.configuration;

import com.java.tms.web.resolver.CurrentUsernameArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Registers {@link CurrentUsernameArgumentResolver} with Spring MVC so that
 * {@code @CurrentUsername} parameters are resolved on every request (AC6-AC8).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUsernameArgumentResolver());
    }
}
