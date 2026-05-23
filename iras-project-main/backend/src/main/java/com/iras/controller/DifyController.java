/**
 * @file DifyController.java
 * @description Dify AI 服务控制器。
 *              提供职能画像分析和简历智能诊断两个 AI 接口。
 *              将前端请求转发到 Dify AI 平台的 Workflow API。
 *              诊断完成后自动保存诊断记录到数据库。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iras.dto.Result;
import com.iras.entity.DiagnosisRecord;
import com.iras.entity.User;
import com.iras.mapper.UserMapper;
import com.iras.service.DiagnosisService;
import com.iras.service.DifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dify")
@RequiredArgsConstructor
public class DifyController {

    private final DifyService difyService;
    private final DiagnosisService diagnosisService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    /**
     * 职能画像接口 - 接收职位名称，转发到 Dify AI 平台进行分析。
     */
    @PostMapping("/job-profile")
    public Result<String> getJobProfile(@RequestBody Map<String, String> request) {
        String jobName = request.get("job_name");
        if (jobName == null || jobName.isBlank()) {
            return Result.error(400, "职位名称不能为空");
        }
        try {
            log.info("调用职能画像 API, jobName={}", jobName);
            String result = difyService.getJobProfile(jobName);
            return Result.success(result);
        } catch (Exception e) {
            log.error("职能画像 API 调用失败", e);
            return Result.error(500, "AI 分析失败: " + e.getMessage());
        }
    }

    /**
     * 智能诊断接口 - 接收简历文件，转发到 Dify AI 平台进行诊断分析。
     * 诊断完成后自动将结果保存到诊断历史记录表。
     */
    @PostMapping("/diagnose")
    public Result<String> diagnoseResume(@RequestParam("file") MultipartFile file,
                                         Authentication authentication) {
        if (file.isEmpty()) {
            return Result.error(400, "请上传简历文件");
        }
        try {
            log.info("调用智能诊断 API, filename={}, size={}", file.getOriginalFilename(), file.getSize());
            String result = difyService.diagnoseResume(file);

            // 诊断成功后保存记录
            try {
                saveDiagnosisRecord(file.getOriginalFilename(), result, authentication);
            } catch (Exception e) {
                // 保存记录失败不影响诊断结果的返回
                log.warn("保存诊断记录失败", e);
            }

            return Result.success(result);
        } catch (Exception e) {
            log.error("智能诊断 API 调用失败", e);
            return Result.error(500, "AI 诊断失败: " + e.getMessage());
        }
    }

    /**
     * 保存诊断记录到数据库。
     * <p>
     * 从 AI 返回的结果中提取 think 标签内容和匹配数量，
     * 构建 DiagnosisRecord 并持久化。
     * </p>
     */
    private void saveDiagnosisRecord(String filename, String result, Authentication authentication) {
        // 获取当前用户
        String username = (String) authentication.getPrincipal();
        User user = userMapper.findByUsername(username);
        if (user == null) return;

        // 提取 think 标签内容
        String thinkContent = null;
        String cleanResult = result;
        java.util.regex.Matcher thinkMatcher = java.util.regex.Pattern
                .compile("(?s)<think>(.*?)</think>").matcher(result);
        if (thinkMatcher.find()) {
            thinkContent = thinkMatcher.group(1).trim();
            cleanResult = result.replaceFirst("(?s)<think>.*?</think>", "").trim();
        }

        // 尝试解析匹配数量
        int matchCount = 0;
        try {
            String jsonStr = cleanResult;
            // 尝试从嵌套结构中提取
            JsonNode root = objectMapper.readTree(jsonStr);
            if (root.isArray()) {
                matchCount = root.size();
            } else if (root.has("matches")) {
                matchCount = root.get("matches").size();
            } else if (root.has("result")) {
                String inner = root.get("result").asText();
                JsonNode innerNode = objectMapper.readTree(inner);
                matchCount = innerNode.isArray() ? innerNode.size() : 1;
            } else {
                matchCount = 1;
            }
        } catch (Exception e) {
            // 非 JSON 格式，无法计算匹配数
            matchCount = 0;
        }

        // 构建并保存记录
        DiagnosisRecord record = new DiagnosisRecord();
        record.setUserId(user.getId());
        record.setResumeFilename(filename);
        record.setDiagnosisResult(cleanResult);
        record.setThinkContent(thinkContent);
        record.setMatchCount(matchCount);
        record.setCreateTime(LocalDateTime.now());
        diagnosisService.saveRecord(record);
        log.info("诊断记录已保存, userId={}, filename={}", user.getId(), filename);
    }
}
