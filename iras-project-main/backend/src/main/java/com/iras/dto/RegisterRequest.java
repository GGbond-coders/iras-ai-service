/**
 * @file RegisterRequest.java
 * @description 注册请求 DTO（数据传输对象）。
 *              封装用户注册时提交的参数，包括用户名、密码和可选的邮箱。
 *              使用 JSR-303 注解进行参数校验。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 注册请求数据传输对象。
 * <p>
 * 包含注册所需的字段：
 * <ul>
 *   <li>username - 用户名（必填）</li>
 *   <li>password - 密码（必填）</li>
 *   <li>email - 邮箱（选填）</li>
 * </ul>
 * </p>
 */
@Data
public class RegisterRequest {

    /** 用户名，注册后不可修改 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 用户密码，存储时会使用 BCrypt 加密 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 联系邮箱，选填 */
    private String email;
}
