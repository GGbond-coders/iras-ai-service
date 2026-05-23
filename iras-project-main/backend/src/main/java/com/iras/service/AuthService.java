/**
 * @file AuthService.java
 * @description 认证服务接口。
 *              定义用户注册和登录的业务方法签名。
 *              具体实现由 {@link com.iras.service.impl.AuthServiceImpl} 提供。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.service;

import com.iras.dto.LoginRequest;
import com.iras.dto.RegisterRequest;
import com.iras.dto.Result;

import java.util.Map;

/**
 * 认证服务接口。
 * <p>
 * 定义用户认证相关的业务方法：
 * <ul>
 *   <li>register - 用户注册</li>
 *   <li>login - 用户登录</li>
 * </ul>
 * 返回类型为 {@link Result}，包含 JWT Token 和用户基本信息。
 * </p>
 */
public interface AuthService {

    /**
     * 用户注册。
     *
     * @param request 注册请求参数（用户名、密码、邮箱）
     * @return 注册结果，成功时包含 JWT Token 和用户信息
     */
    Result<Map<String, Object>> register(RegisterRequest request);

    /**
     * 用户登录。
     *
     * @param request 登录请求参数（用户名/邮箱、密码）
     * @return 登录结果，成功时包含 JWT Token 和用户信息
     */
    Result<Map<String, Object>> login(LoginRequest request);
}
