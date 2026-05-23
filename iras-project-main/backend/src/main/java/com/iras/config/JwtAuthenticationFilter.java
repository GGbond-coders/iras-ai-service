/**
 * @file JwtAuthenticationFilter.java
 * @description JWT 认证过滤器。
 *              拦截每个 HTTP 请求，从 Authorization 请求头中提取 JWT Token，
 *              验证 Token 的有效性，并将认证信息设置到 Spring Security 上下文中。
 *              该过滤器在每次请求时只执行一次（OncePerRequestFilter）。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.config;

import com.iras.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器。
 * <p>
 * 继承 {@link OncePerRequestFilter}，确保每个请求只经过一次过滤逻辑。
 * 主要职责：
 * <ol>
 *   <li>从请求头中提取 Bearer Token</li>
 *   <li>验证 Token 的合法性和有效性</li>
 *   <li>解析 Token 中的用户名</li>
 *   <li>构建认证对象并存入 SecurityContext</li>
 * </ol>
 * </p>
 */
@Component
@RequiredArgsConstructor  // Lombok 自动生成包含 final 字段的构造函数
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT 工具类，用于 Token 的解析和验证 */
    private final JwtUtil jwtUtil;

    /**
     * 过滤器核心逻辑。
     * <p>
     * 对每个 HTTP 请求执行以下操作：
     * 1. 调用 {@link #extractToken} 提取 JWT Token
     * 2. 验证 Token 是否有效
     * 3. 如果有效，解析用户名并创建认证对象
     * 4. 将认证对象存入 {@link SecurityContextHolder}
     * 5. 无论是否认证成功，都继续执行后续过滤器链
     * </p>
     *
     * @param request     HTTP 请求对象
     * @param response    HTTP 响应对象
     * @param filterChain 过滤器链，用于继续执行后续过滤器
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 从请求头中提取 JWT Token
        String token = extractToken(request);

        // 验证 Token 有效性，如果有效则设置认证信息
        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            // 从 Token 中解析用户名
            String username = jwtUtil.getUsernameFromToken(token);
            // 创建认证令牌（用户名、密码、权限列表），此处无密码和权限
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
            // 将认证信息存入 Security 上下文，后续可通过 SecurityContextHolder 获取当前用户
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 继续执行过滤器链中的下一个过滤器
        filterChain.doFilter(request, response);
    }

    /**
     * 从 HTTP 请求头中提取 JWT Token。
     * <p>
     * 期望请求头格式为：{@code Authorization: Bearer <token>}
     * </p>
     *
     * @param request HTTP 请求对象
     * @return 提取到的 JWT Token 字符串，如果不存在或格式不正确则返回 null
     */
    private String extractToken(HttpServletRequest request) {
        // 获取 Authorization 请求头
        String bearerToken = request.getHeader("Authorization");
        // 检查是否以 "Bearer " 开头
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // 截取 "Bearer " 之后的 Token 部分（第 7 位开始）
            return bearerToken.substring(7);
        }
        return null;
    }
}
