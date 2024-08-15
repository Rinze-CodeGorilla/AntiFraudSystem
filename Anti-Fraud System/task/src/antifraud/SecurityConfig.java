package antifraud;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

@Configuration
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "servlet", matchIfMissing = true)
public class SecurityConfig {
    @Autowired
    RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Bean
    @Order(2)
    public SecurityFilterChain swaggerFilter(HttpSecurity http) throws Exception {
        return http.securityMatcher("/swagger-ui*/**", "/v3/api-docs/**").authorizeHttpRequests(s -> s.anyRequest().permitAll()).build();
    }

    @Bean
    OpenAPI customOpenAPI() {
        return new OpenAPI().components(new Components().addSecuritySchemes("basic", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")));
    }

    @Bean
    @Order(1)
    @ConditionalOnExpression("'${spring.datasource.url}'.startsWith('jdbc:h2:')")
    public SecurityFilterChain h2Filter(HttpSecurity http) throws Exception {
        IpAddressMatcher a = new IpAddressMatcher("127.0.0.1");
        IpAddressMatcher b = new IpAddressMatcher("[::1]");
        return http
                .securityMatcher(PathRequest.toH2Console())
                .csrf().disable()
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("frame-ancestors 'self'")
                                .policyDirectives("frame-src 'self'")
                        )
                )
                .authorizeHttpRequests(r -> r
                        .anyRequest().access(
                                (auth, req) -> new AuthorizationDecision(
                                        a.matches(req.getRequest()) || b.matches(req.getRequest())
                                )
                        )
                )
                .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .httpBasic(Customizer.withDefaults())
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf().disable()
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, "/api/auth/user").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/auth/user/{username}").hasRole(Role.ADMINISTRATOR.name())
                        .requestMatchers(HttpMethod.GET, "/api/auth/list").hasAnyRole(Role.ADMINISTRATOR.name(), Role.SUPPORT.name())
                        .requestMatchers(HttpMethod.POST, "/api/antifraud/transaction").hasRole(Role.MERCHANT.name())

                        .requestMatchers(HttpMethod.PUT, "/api/auth/role").hasRole(Role.ADMINISTRATOR.name())
                        .requestMatchers(HttpMethod.PUT, "/api/auth/access").hasRole(Role.ADMINISTRATOR.name())

                        .requestMatchers(HttpMethod.GET, "/api/antifraud/suspicious-ip").hasRole(Role.SUPPORT.name())
                        .requestMatchers(HttpMethod.POST, "/api/antifraud/suspicious-ip").hasRole(Role.SUPPORT.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/antifraud/suspicious-ip/{ip}").hasRole(Role.SUPPORT.name())
                        .requestMatchers(HttpMethod.GET, "/api/antifraud/stolencard").hasRole(Role.SUPPORT.name())
                        .requestMatchers(HttpMethod.POST, "/api/antifraud/stolencard").hasRole(Role.SUPPORT.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/antifraud/stolencard/{number}").hasRole(Role.SUPPORT.name())
                        .requestMatchers(HttpMethod.GET, "/api/antifraud/history").hasRole(Role.SUPPORT.name())
                        .requestMatchers(HttpMethod.GET, "/api/antifraud/history/{number}").hasRole(Role.SUPPORT.name())
                        .requestMatchers(HttpMethod.PUT, "/api/antifraud/transaction").hasRole(Role.SUPPORT.name())

                        .requestMatchers("/actuator/shutdown").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()

                        .anyRequest().denyAll()
                )
                .build();
    }

}
