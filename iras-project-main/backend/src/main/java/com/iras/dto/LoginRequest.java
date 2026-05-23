/**
 * @file LoginRequest.java
 * @description 登录请求 DTO（数据传输对象）。
 *              封装用户登录时提交的参数，支持用户名或邮箱登录。
 *              使用 JSR-303 注解进行参数校验。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求数据传输对象。
 * <p>
 * 包含登录所需的两个必填字段：
 * <ul>
 *   <li>username - 用户名或邮箱（登录时自动识别）</li>
 *   <li>password - 用户密码</li>
 * </ul>
 * 使用 {@code @NotBlank} 注解确保字段不为空。
 * </p>
 */
@Data  // Lombok 自动生成 getter/setter/toString/equals/hashCode
public class LoginRequest {

    /** 用户名或邮箱，支持两种方式登录 */
    @NotBlank(message = "用户名或邮箱不能为空")
    private String username;

    /** 用户密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
