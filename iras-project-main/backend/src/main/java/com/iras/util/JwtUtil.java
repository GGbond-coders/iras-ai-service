/**
 * @file JwtUtil.java
 * @description JWT（JSON Web Token）工具类。
 *              提供 Token 的生成、解析和验证功能。
 *              使用 HMAC-SHA512 算法签名，确保 Token 的完整性和不可篡改性。
 *              密钥和过期时间从 application.yml 配置文件中读取。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类。
 * <p>
 * 基于 jjwt 库实现，提供三个核心方法：
 * <ul>
 *   <li>{@link #generateToken} - 生成 JWT Token</li>
 *   <li>{@link #getUsernameFromToken} - 从 Token 中解析用户名</li>
 *   <li>{@link #validateToken} - 验证 Token 的有效性</li>
 * </ul>
 * Token 结构：Header.Payload.Signature
 * <ul>
 *   <li>Header: 算法类型（HS512）</li>
 *   <li>Payload: 用户名（subject）、签发时间、过期时间</li>
 *   <li>Signature: HMAC-SHA512 签名</li>
 * </ul>
 * </p>
 */
@Component  // 声明为 Spring 组件
public class JwtUtil {

    /** JWT 签名密钥（从配置文件注入，需至少 64 字节以满足 HS512 要求） */
    @Value("${jwt.secret}")
    private String secret;

    /** Token 过期时间（毫秒），从配置文件注入，默认 24 小时 */
    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * 获取 HMAC-SHA512 签名密钥。
     * <p>
     * 将配置文件中的密钥字符串转换为 SecretKey 对象。
     * </p>
     *
     * @return SecretKey 签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token。
     * <p>
     * Token 包含以下声明（Claims）：
     * <ul>
     *   <li>subject - 用户名</li>
     *   <li>issuedAt - 签发时间</li>
     *   <li>expiration - 过期时间</li>
     * </ul>
     * </p>
     *
     * @param username 用户名（作为 Token 的 subject）
     * @return 签名后的 JWT Token 字符串
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)                                            // 设置主题（用户名）
                .issuedAt(new Date())                                         // 设置签发时间
                .expiration(new Date(System.currentTimeMillis() + expiration)) // 设置过期时间
                .signWith(getSigningKey())                                    // 使用 HMAC-SHA512 签名
                .compact();                                                   // 生成最终 Token 字符串
    }

    /**
     * 从 JWT Token 中解析用户名。
     * <p>
     * 验证签名后提取 Payload 中的 subject 字段（即用户名）。
     * </p>
     *
     * @param token JWT Token 字符串
     * @return Token 中包含的用户名
     * @throws JwtException Token 无效或已过期时抛出异常
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())       // 设置签名验证密钥
                .build()
                .parseSignedClaims(token)          // 解析并验证签名
                .getPayload()
                .getSubject();                     // 提取 subject（用户名）
    }

    /**
     * 验证 JWT Token 的有效性。
     * <p>
     * 检查项包括：
     * <ul>
     *   <li>签名是否正确</li>
     *   <li>Token 是否已过期</li>
     *   <li>Token 格式是否合法</li>
     * </ul>
     * </p>
     *
     * @param token JWT Token 字符串
     * @return true 表示 Token 有效，false 表示无效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())   // 设置签名验证密钥
                    .build()
                    .parseSignedClaims(token);     // 尝试解析并验证
            return true;                           // 解析成功则 Token 有效
        } catch (JwtException | IllegalArgumentException e) {
            return false;  // 捕获 JWT 异常或参数异常，Token 无效
        }
    }
}
