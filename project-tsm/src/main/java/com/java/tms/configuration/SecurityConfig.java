package com.java.tms.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Security rules for the TMS backend.
 *
 * <p>Two independent authentication mechanisms are configured in two ordered
 * {@link SecurityFilterChain} beans:
 * <ul>
 *     <li>{@code /api/**} — stateless JWT resource server (direct API clients, Postman, curl)</li>
 *     <li>everything else — OIDC login against Keycloak for the browser UI</li>
 * </ul>
 *
 * <p>Access is restricted in two independent ways, as required by the lab:
 * <ul>
 *     <li>{@link SecurityFilterChain} rules below (e.g. only ADMIN may POST/PUT/DELETE
 *         a project) — enforced before the request reaches any controller;</li>
 *     <li>{@code @PreAuthorize} on {@code TranslationService#approve}, which additionally
 *         restricts translation approval to EDITOR regardless of the filter chain rules.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()

                        // Project management is ADMIN-only — enforced here, at SecurityFilterChain level (AC6).
                        .requestMatchers(HttpMethod.POST, "/api/projects").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/projects/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/projects/*").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/glossary").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/glossary/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/glossary/*").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/projects/*/text-keys").hasAnyRole("ADMIN", "TRANSLATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/projects/*/text-keys/*").hasAnyRole("ADMIN", "TRANSLATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/projects/*/text-keys/*").hasAnyRole("ADMIN", "TRANSLATOR")

                        .requestMatchers(HttpMethod.POST, "/api/text-keys/*/translations").hasAnyRole("ADMIN", "TRANSLATOR")
                        // A translator may only PUT/DELETE their *own* proposal — that ownership
                        // check happens in TranslationService, since the filter chain only sees
                        // roles, not who authored which row.
                        .requestMatchers(HttpMethod.PUT, "/api/text-keys/*/translations/*").hasAnyRole("ADMIN", "TRANSLATOR")

                        // Approval is intentionally left at "authenticated" here: the ADMIN/EDITOR-only
                        // restriction for it is enforced independently via @PreAuthorize (AC7), so this
                        // endpoint demonstrates that method security works even without a filter-chain rule.
                        .requestMatchers(HttpMethod.PATCH, "/api/translations/*/approve").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/translations/*").hasAnyRole("ADMIN", "EDITOR", "TRANSLATOR")

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain uiSecurityFilterChain(HttpSecurity http,
                                                     @Value("${app.security.logout-url}") String keycloakLogoutUrl) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // /ui/token is permitAll on purpose: TokenController answers 401 itself for
                        // anonymous callers, so the UI's fetch() is not redirected to the Keycloak login page.
                        .anyRequest().permitAll()
                )
                // After a successful OIDC login the user lands back on the application UI.
                .oauth2Login(oauth2 -> oauth2.defaultSuccessUrl("/tsm.html", true))
                .logout(logout -> logout
                        // The static UI logs out with a plain link (GET), so allow GET here.
                        .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/logout"))
                        // Also end the Keycloak SSO session, otherwise "Login" would silently sign the user in again.
                        .logoutSuccessHandler((request, response, authentication) -> response.sendRedirect(keycloakLogoutUrl))
                );

        return http.build();
    }

    /**
     * Keycloak puts realm roles into the {@code realm_access.roles} claim, which Spring
     * does not read by default. This converter maps them to {@code ROLE_*} authorities
     * so that {@code hasRole(...)} in the filter chain and {@code @PreAuthorize} work.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new java.util.HashSet<>(scopeConverter.convert(jwt));

            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
                authorities.addAll(roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toSet()));
            }
            return authorities;
        });
        return converter;
    }
}
