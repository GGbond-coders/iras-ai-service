/**
 * @file AuthServiceImpl.java
 * @description 认证服务实现类。
 *              实现用户注册和登录的完整业务逻辑，包括：
 *              - 注册时的用户名/邮箱重复检查、密码加密、Token 生成
 *              - 登录时的用户查找（支持用户名/邮箱）、密码验证、Token 生成
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.service.impl;

import com.iras.dto.LoginRequest;
import com.iras.dto.RegisterRequest;
import com.iras.dto.Result;
import com.iras.entity.User;
import com.iras.mapper.UserMapper;
import com.iras.service.AuthService;
import com.iras.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务实现类。
 * <p>
 * 核心业务流程：
 * <ul>
 *   <li>注册：检查唯一性 -> 加密密码 -> 插入数据库 -> 生成 JWT Token -> 返回用户信息</li>
 *   <li>登录：查找用户 -> 验证密码 -> 生成 JWT Token -> 返回用户信息</li>
 * </ul>
 * </p>
 */
@Service            // 声明为 Spring Service 组件
@RequiredArgsConstructor  // 自动注入 final 字段
public class AuthServiceImpl implements AuthService {

    /** 用户数据访问层 */
    private final UserMapper userMapper;

    /** 密码编码器（BCrypt） */
    private final PasswordEncoder passwordEncoder;

    /** JWT 工具类，用于生成和验证 Token */
    private final JwtUtil jwtUtil;

    /**
     * 用户注册实现。
     * <p>
     * 业务流程：
     * 1. 检查用户名是否已存在（唯一约束）
     * 2. 如果提供了邮箱，检查邮箱是否已被注册
     * 3. 使用 BCrypt 加密密码
     * 4. 将用户信息插入数据库
     * 5. 生成 JWT Token
     * 6. 返回 Token 和用户基本信息
     * </p>
     *
     * @param request 注册请求参数
     * @return 注册结果（包含 Token 和用户信息）
     */
    @Override
    public Result<Map<String, Object>> register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (userMapper.findByUsername(request.getUsername()) != null) {
            return Result.error(400, "用户名已存在");
        }

        // 检查邮箱是否已被使用（仅在提供了邮箱时检查）
        if (request.getEmail() != null && userMapper.findByEmail(request.getEmail()) != null) {
            return Result.error(400, "邮箱已被注册");
        }

        // 创建用户实体并设置属性
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));  // BCrypt 加密密码
        user.setEmail(request.getEmail());
        userMapper.insert(user);  // 插入数据库，自增 ID 会回填到 user 对象

        // 生成 JWT Token
        String token = jwtUtil.generateToken(user.getUsername());
        // 构建返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("role", user.getRole() != null ? user.getRole() : "user");

        return Result.success("注册成功", data);
    }

    /**
     * 用户登录实现。
     * <p>
     * 业务流程：
     * 1. 先按用户名查找用户
     * 2. 如果未找到，再按邮箱查找（支持用户名或邮箱登录）
     * 3. 使用 BCrypt 验证密码
     * 4. 生成 JWT Token
     * 5. 返回 Token 和用户基本信息
     * </p>
     *
     * @param request 登录请求参数
     * @return 登录结果（包含 Token 和用户信息）
     */
    @Override
    public Result<Map<String, Object>> login(LoginRequest request) {
        // 支持用户名或邮箱登录：先按用户名查找
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            // 用户名未找到，尝试按邮箱查找
            user = userMapper.findByEmail(request.getUsername());
        }

        // 用户不存在
        if (user == null) {
            return Result.error(401, "用户名或密码错误");
        }

        // 验证密码（BCrypt 比较）
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return Result.error(401, "用户名或密码错误");
        }

        // 生成 JWT Token
        String token = jwtUtil.generateToken(user.getUsername());
        // 构建返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("role", user.getRole() != null ? user.getRole() : "user");

        return Result.success("登录成功", data);
    }
}
