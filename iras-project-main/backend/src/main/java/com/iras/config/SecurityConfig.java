/**
 * @file SecurityConfig.java
 * @description Spring Security 安全配置类。
 *              配置安全过滤器链、密码编码器、认证管理器和 CORS 跨域策略。
 *              采用无状态（STATELESS）会话管理模式，配合 JWT 实现认证。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iras.dto.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 安全配置类。
 * <p>
 * 核心配置项：
 * <ul>
 *   <li>禁用 CSRF（因为使用 JWT 无状态认证，无需 CSRF 保护）</li>
 *   <li>启用 CORS 跨域资源共享</li>
 *   <li>设置无状态会话管理（不使用 HttpSession）</li>
 *   <li>配置请求授权规则（公开接口 vs 需认证接口）</li>
 *   <li>自定义未认证处理（返回 401 JSON 响应）</li>
 *   <li>将 JWT 过滤器添加到 UsernamePasswordAuthenticationFilter 之前</li>
 * </ul>
 * </p>
 */
@Configuration       // 声明为配置类
@EnableWebSecurity   // 启用 Spring Web 安全功能
@RequiredArgsConstructor
public class SecurityConfig {

    /** JWT 认证过滤器，在请求到达认证过滤器之前执行 */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** Jackson JSON 序列化器，用于将错误信息写入响应体 */
    private final ObjectMapper objectMapper;

    /**
     * 配置安全过滤器链。
     * <p>
     * 定义了完整的安全策略：
     * - CSRF 禁用
     * - CORS 启用
     * - 无状态会话
     * - 接口权限控制
     * - 自定义认证入口
     * - JWT 过滤器位置
     * </p>
     *
     * @param http HttpSecurity 配置对象
     * @return 构建完成的 SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF 防护（JWT 无状态认证不需要）
                .csrf(AbstractHttpConfigurer::disable)
                // 启用 CORS 跨域配置
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 设置会话管理为无状态（不创建 HttpSession）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置请求授权规则
                .authorizeHttpRequests(auth -> auth
                        // 放行认证相关接口（登录、注册）
                        .requestMatchers("/api/auth/**").permitAll()
                        // 放行所有 OPTIONS 预检请求（CORS 预检）
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )
                // 自定义异常处理
                .exceptionHandling(ex -> ex
                        // 自定义未认证入口点：返回 401 JSON 响应
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 设置 401 状态码
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE); // 设置 JSON 内容类型
                            response.setCharacterEncoding("UTF-8");                    // 设置字符编码
                            // 写入统一错误响应格式
                            objectMapper.writeValue(response.getWriter(), Result.error(401, "未登录或Token已过期"));
                        })
                )
                // 在 UsernamePasswordAuthenticationFilter 之前添加 JWT 认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 配置密码编码器。
     * <p>
     * 使用 BCrypt 强哈希算法对密码进行加密存储。
     * BCrypt 会自动生成随机盐值，并将其嵌入到哈希结果中。
     * </p>
     *
     * @return BCryptPasswordEncoder 密码编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置认证管理器。
     * <p>
     * 从 {@link AuthenticationConfiguration} 中获取默认的认证管理器，
     * 用于处理用户认证请求（如登录时验证用户名密码）。
     * </p>
     *
     * @param config 认证配置对象
     * @return AuthenticationManager 认证管理器实例
     * @throws Exception 获取异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 配置 CORS（跨域资源共享）策略。
     * <p>
     * 允许所有来源、常用 HTTP 方法和请求头，支持携带凭证（Cookie），
     * 预检请求缓存时间为 3600 秒（1 小时）。
     * </p>
     *
     * @return CorsConfigurationSource CORS 配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));                              // 允许所有来源模式
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); // 允许的 HTTP 方法
        config.setAllowedHeaders(List.of("*"));                                      // 允许所有请求头
        config.setAllowCredentials(true);                                            // 允许携带凭证
        config.setMaxAge(3600L);                                                     // 预检缓存 1 小时

        // 基于 URL 的 CORS 配置源，对所有路径应用上述配置
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
