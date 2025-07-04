package com.example.ootd.config;

import com.example.ootd.security.CustomUserDetailsService;
import com.example.ootd.security.CustomUsernamePasswordAuthenticationFilter;
import com.example.ootd.security.jwt.JwtAuthenticationFilter;
import com.example.ootd.security.jwt.JwtService;
import com.example.ootd.security.oauth2.CustomOAuth2UserService;
import com.example.ootd.security.oauth2.OAuth2LoginSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
//import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  /**
   * 기초 세팅을 위한 임시 필터체인
   */
  @Bean
  public SecurityFilterChain chain(HttpSecurity http,
      AuthenticationManager manager,
      CustomUsernamePasswordAuthenticationFilter customFilter,
      JwtAuthenticationFilter jwtFilter,
      OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
      CustomOAuth2UserService customOAuth2UserService) throws Exception {
    customFilter.setAuthenticationManager(manager);

    http
        .exceptionHandling(e -> e
            .authenticationEntryPoint((request, response, authException) -> {
              response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            })
        )
        .csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.requestMatchers(
                    "/",
                    "/assets/**",
                    "/static/**",
                    "/favicon.ico",
                    "/closet-hanger-logo.png",
                    "/index.html",
                    "/vite.svg"
                ).permitAll()
            .requestMatchers("/oauth2/callback").permitAll()
            .requestMatchers("/api/auth/me").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
            .requestMatchers("/test/me").hasRole("USER")
            .requestMatchers(HttpMethod.POST, "/api/clothes/attribute-defs").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/clothes/attribute-defs/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PATCH, "/api/clothes/attribute-defs/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2.userInfoEndpoint(userInfo ->
                userInfo.userService(customOAuth2UserService))
            .successHandler(oAuth2LoginSuccessHandler)
            .failureHandler((request, response, exception) -> {
              // 1) 로그
              log.error("OAuth2 로그인 실패 원인:", exception);
            }))
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAt(customFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public AuthenticationManager authManager(HttpSecurity http, PasswordEncoder passwordEncoder,
      CustomUserDetailsService userDetailsService)
      throws Exception {

    AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
    builder
        .userDetailsService(userDetailsService)
        .passwordEncoder(passwordEncoder);

    return builder.build();
  }

  @Bean
  public SessionRegistry sessionRegistry() {
    return new SessionRegistryImpl();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public RoleHierarchy roleHierarchy() {
    String hierarchyString = """
        ROLE_ADMIN > ROLE_USER
        """;

    return RoleHierarchyImpl.fromHierarchy(hierarchyString);
  }


}
