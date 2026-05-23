/**
 * @file AdminController.java
 * @description 管理员控制器。
 *              提供管理后台的 REST API，包括用户管理、职位管理和系统统计。
 *              所有接口需要管理员角色才能访问。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.controller;

import com.iras.dto.PageResult;
import com.iras.dto.Result;
import com.iras.entity.JobInfo;
import com.iras.entity.User;
import com.iras.mapper.UserMapper;
import com.iras.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserMapper userMapper;

    /** AI 服务地址（从 dify.base-url 派生，去掉 /v1 后缀） */
    @Value("${dify.base-url}")
    private String aiBaseUrl;

    /** AI 服务认证 Token */
    @Value("${dify.job-profile-key}")
    private String aiToken;

    /**
     * 校验当前用户是否为管理员，非管理员返回 null。
     */
    private User requireAdmin(Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        User user = userMapper.findByUsername(username);
        if (user == null || !"admin".equals(user.getRole())) {
            return null;
        }
        return user;
    }

    // ==================== 用户管理 ====================

    /** 获取用户列表（分页）- 仅管理员 */
    @GetMapping("/users")
    public Result<PageResult<User>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        if (requireAdmin(authentication) == null) return Result.error(403, "无权限");
        return Result.success(adminService.getUsers(page, size));
    }

    /** 修改用户角色 - 仅管理员 */
    @PutMapping("/users/{id}/role")
    public Result<String> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> body,
                                         Authentication authentication) {
        if (requireAdmin(authentication) == null) return Result.error(403, "无权限");
        String role = body.get("role");
        if (role == null || (!role.equals("user") && !role.equals("admin"))) {
            return Result.error(400, "角色值无效，仅支持 user 或 admin");
        }
        if (adminService.updateUserRole(id, role)) {
            return Result.success("修改成功", null);
        }
        return Result.error(400, "修改失败");
    }

    /** 删除用户 - 仅管理员 */
    @DeleteMapping("/users/{id}")
    public Result<String> deleteUser(@PathVariable Long id, Authentication authentication) {
        User admin = requireAdmin(authentication);
        if (admin == null) return Result.error(403, "无权限");
        // 防止管理员删除自己
        if (admin.getId().equals(id)) {
            return Result.error(400, "不能删除当前登录用户");
        }
        if (adminService.deleteUser(id)) {
            return Result.success("删除成功", null);
        }
        return Result.error(400, "删除失败");
    }

    // ==================== 职位管理 ====================

    /** 获取职位列表（分页）- 仅管理员 */
    @GetMapping("/jobs")
    public Result<PageResult<JobInfo>> getJobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        if (requireAdmin(authentication) == null) return Result.error(403, "无权限");
        return Result.success(adminService.getJobs(page, size));
    }

    /** 新增职位 - 仅管理员 */
    @PostMapping("/jobs")
    public Result<String> addJob(@RequestBody JobInfo job, Authentication authentication) {
        if (requireAdmin(authentication) == null) return Result.error(403, "无权限");
        adminService.addJob(job);
        return Result.success("添加成功", null);
    }

    /** 更新职位 - 仅管理员 */
    @PutMapping("/jobs/{id}")
    public Result<String> updateJob(@PathVariable Long id, @RequestBody JobInfo job,
                                    Authentication authentication) {
        if (requireAdmin(authentication) == null) return Result.error(403, "无权限");
        job.setId(id);
        if (adminService.updateJob(job)) {
            return Result.success("更新成功", null);
        }
        return Result.error(400, "更新失败");
    }

    /** 删除职位 - 仅管理员 */
    @DeleteMapping("/jobs/{id}")
    public Result<String> deleteJob(@PathVariable Long id, Authentication authentication) {
        if (requireAdmin(authentication) == null) return Result.error(403, "无权限");
        if (adminService.deleteJob(id)) {
            return Result.success("删除成功", null);
        }
        return Result.error(400, "删除失败");
    }

    // ==================== 系统统计 ====================

    /** 获取系统统计数据 - 仅管理员 */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(Authentication authentication) {
        if (requireAdmin(authentication) == null) return Result.error(403, "无权限");
        return Result.success(adminService.getStatistics());
    }

    // ==================== AI 服务管理 ====================

    /**
     * 增量同步 ChromaDB 向量库，代理到 Python AI 服务。
     * 仅同步有变化的岗位，不清空已有向量。
     */
    @PostMapping("/chromadb/sync")
    public Result<String> syncChromaDb(Authentication authentication) {
        if (requireAdmin(authentication) == null) return Result.error(403, "无权限");
        try {
            String raw = callAiAdmin("/admin/chromadb/sync");
            // Parse Python service response and check for errors
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(raw);
            String status = node.has("status") ? node.get("status").asText() : "";
            String message = node.has("message") ? node.get("message").asText() : raw;
            if ("error".equals(status)) {
                return Result.error(500, "同步失败: " + message);
            }
            return Result.success(message, null);
        } catch (Exception e) {
            log.error("ChromaDB sync failed: {}", e.getMessage());
            return Result.error(500, "向量库同步失败: " + e.getMessage());
        }
    }

    /**
     * 调用 AI 服务的管理接口（非 /v1 路径）。
     * 从 dify.base-url（如 http://localhost:8000/v1）派生根地址，拼接管理路径。
     */
    private String callAiAdmin(String path) throws Exception {
        String rootUrl = aiBaseUrl.replaceFirst("/v1$", "");
        URL url = new URL(rootUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + aiToken);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(600000);   // 10 min — ChromaDB sync may take several minutes
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write("{}".getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(code == 200 ? conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        if (code != 200) {
            throw new RuntimeException("AI service returned " + code + ": " + response);
        }
        return response.toString();
    }
}
