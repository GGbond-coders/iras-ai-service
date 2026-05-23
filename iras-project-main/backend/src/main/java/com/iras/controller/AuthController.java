/**
 * @file AuthController.java
 * @description 认证控制器。
 *              提供用户注册和登录的 REST API 接口。
 *              路径前缀为 /api/auth，所有接口无需认证即可访问。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.controller;

import com.iras.dto.LoginRequest;
import com.iras.dto.RegisterRequest;
import com.iras.dto.Result;
import com.iras.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器。
 * <p>
 * 处理用户认证相关的 HTTP 请求，包括：
 * <ul>
 *   <li>用户注册（POST /api/auth/register）</li>
 *   <li>用户登录（POST /api/auth/login）</li>
 * </ul>
 * 请求参数通过 {@code @Valid} 注解进行 JSR-303 校验。
 * </p>
 */
@RestController                    // 声明为 REST 控制器，返回 JSON 响应
@RequestMapping("/api/auth")       // 映射请求路径前缀
@RequiredArgsConstructor           // Lombok 自动生成构造函数注入
public class AuthController {

    /** 认证服务，处理注册和登录的业务逻辑 */
    private final AuthService authService;

    /**
     * 用户注册接口。
     * <p>
     * 接收注册请求，校验参数合法性后调用服务层完成注册。
     * 注册成功后返回 JWT Token 和用户基本信息。
     * </p>
     *
     * @param request 注册请求对象，包含用户名、密码和可选的邮箱，通过 @Valid 进行参数校验
     * @return 统一响应结果，包含 JWT Token 和用户信息
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * 用户登录接口。
     * <p>
     * 接收登录请求，支持用户名或邮箱登录。
     * 登录成功后返回 JWT Token 和用户基本信息。
     * </p>
     *
     * @param request 登录请求对象，包含用户名/邮箱和密码，通过 @Valid 进行参数校验
     * @return 统一响应结果，包含 JWT Token 和用户信息
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
