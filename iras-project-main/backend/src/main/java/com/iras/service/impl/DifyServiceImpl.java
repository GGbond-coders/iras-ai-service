/**
 * @file DifyServiceImpl.java
 * @description Dify AI 服务实现类。
 *              通过 HTTP 连接直接调用 Dify AI 平台的 Workflow API。
 *              支持两种调用方式：
 *              1. 纯文本输入（职能画像）
 *              2. 文件上传 + Workflow 调用（简历诊断）
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iras.service.DifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Dify AI 服务实现类。
 * <p>
 * 使用原生 {@link HttpURLConnection} 与 Dify AI 平台通信，
 * 支持 JSON 请求和 multipart/form-data 文件上传。
 * 所有 API 调用均使用 blocking（阻塞）模式，等待 AI 完成推理后返回结果。
 * </p>
 */
@Slf4j      // Lombok 日志
@Service    // 声明为 Spring Service 组件
@RequiredArgsConstructor
public class DifyServiceImpl implements DifyService {

    /** Dify API 基础 URL（从配置文件注入） */
    @Value("${dify.base-url}")
    private String baseUrl;

    /** 职能画像 Workflow 的 API Key */
    @Value("${dify.job-profile-key}")
    private String jobProfileKey;

    /** 简历诊断 Workflow 的 API Key */
    @Value("${dify.resume-diagnosis-key}")
    private String resumeDiagnosisKey;

    /** Jackson JSON 对象映射器 */
    private final ObjectMapper objectMapper;

    /**
     * 调用 Dify 职能画像 API。
     * <p>
     * 构建包含职位名称的 workflow 请求，发送到 Dify 的 /workflows/run 接口。
     * </p>
     *
     * @param jobName 职位名称
     * @return AI 生成的职能画像 JSON 字符串
     */
    @Override
    public String getJobProfile(String jobName) {
        String url = baseUrl + "/workflows/run";
        // 构建 workflow 请求体（包含输入参数 job_name）
        String requestBody = buildWorkflowRequest("job_name", jobName);
        // 调用 Dify API
        return callDifyApi(url, requestBody, jobProfileKey);
    }

    /**
     * 调用 Dify 简历诊断 API（文件上传模式）。
     * <p>
     * 分两步执行：
     * 1. 先将简历文件上传到 Dify 平台，获取 upload_file_id
     * 2. 用文件 ID 调用 workflow API 进行诊断分析
     * </p>
     *
     * @param file 上传的简历文件
     * @return AI 生成的诊断报告 JSON 字符串
     */
    @Override
    public String diagnoseResume(MultipartFile file) {
        // 第一步：上传文件到 Dify 平台，获取文件 ID
        String uploadFileId = uploadFile(file);

        // 第二步：用文件 ID 调用 workflow API
        String url = baseUrl + "/workflows/run";
        String requestBody = buildFileWorkflowRequest("resume_text", uploadFileId);
        return callDifyApi(url, requestBody, resumeDiagnosisKey);
    }

    /**
     * 上传文件到 Dify 平台。
     * <p>
     * 使用 multipart/form-data 格式上传文件，包含两个字段：
     * <ul>
     *   <li>file - 文件内容</li>
     *   <li>user - 用户标识（固定为 "iras-user"）</li>
     * </ul>
     * 上传成功后返回 Dify 平台分配的文件 ID。
     * </p>
     *
     * @param file 要上传的文件
     * @return Dify 平台返回的 upload_file_id
     * @throws RuntimeException 上传失败时抛出异常
     */
    private String uploadFile(MultipartFile file) {
        // 生成 multipart 边界字符串
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        String uploadUrl = baseUrl + "/files/upload";

        HttpURLConnection connection = null;
        try {
            URL url = new URL(uploadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + resumeDiagnosisKey);  // 设置认证头
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setDoOutput(true);         // 允许写入请求体
            connection.setConnectTimeout(60000);   // 连接超时 60 秒
            connection.setReadTimeout(60000);      // 读取超时 60 秒

            try (OutputStream os = connection.getOutputStream()) {
                // 写入 file 字段（文件内容）
                os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                os.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" +
                        file.getOriginalFilename() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                os.write(("Content-Type: " + file.getContentType() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                os.write(file.getBytes());  // 写入文件二进制内容
                os.write("\r\n".getBytes(StandardCharsets.UTF_8));

                // 写入 user 字段（用户标识）
                os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                os.write("Content-Disposition: form-data; name=\"user\"\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                os.write("iras-user\r\n".getBytes(StandardCharsets.UTF_8));

                // 写入结束边界
                os.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }

            // 读取响应
            int responseCode = connection.getResponseCode();
            java.io.InputStream inputStream = (responseCode >= 200 && responseCode < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String responseBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            log.info("Dify 文件上传响应: code={}, body={}", responseCode, responseBody);

            if (responseCode >= 200 && responseCode < 300) {
                // 解析响应 JSON，提取文件 ID
                JsonNode root = objectMapper.readTree(responseBody);
                return root.get("id").asText();
            } else {
                throw new RuntimeException("文件上传失败: " + responseCode + " - " + responseBody);
            }
        } catch (RuntimeException e) {
            throw e;  // 运行时异常直接抛出
        } catch (Exception e) {
            log.error("上传文件到 Dify 异常", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        } finally {
            // 释放 HTTP 连接
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * 构建纯文本输入的 workflow 请求体。
     * <p>
     * 请求格式：
     * <pre>
     * {
     *   "inputs": { "key": "value" },
     *   "response_mode": "blocking",
     *   "user": "iras-user"
     * }
     * </pre>
     * </p>
     *
     * @param inputKey   输入参数名
     * @param inputValue 输入参数值
     * @return JSON 格式的请求体字符串
     */
    private String buildWorkflowRequest(String inputKey, String inputValue) {
        try {
            return objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                put("inputs", new java.util.HashMap<>() {{
                    put(inputKey, inputValue);  // 设置输入参数
                }});
                put("response_mode", "streaming");  // 流式模式，避免网关超时
                put("user", "iras-user");           // 用户标识
            }});
        } catch (Exception e) {
            log.error("构建请求体失败", e);
            throw new RuntimeException("构建请求体失败", e);
        }
    }

    /**
     * 构建带文件引用的 workflow 请求体。
     * <p>
     * 与纯文本请求不同，文件引用使用数组格式：
     * <pre>
     * {
     *   "inputs": {
     *     "resume_text": [
     *       { "transfer_method": "local_file", "upload_file_id": "xxx", "type": "document" }
     *     ]
     *   },
     *   "response_mode": "blocking",
     *   "user": "iras-user"
     * }
     * </pre>
     * </p>
     *
     * @param inputKey      输入参数名
     * @param uploadFileId  Dify 平台返回的文件 ID
     * @return JSON 格式的请求体字符串
     */
    private String buildFileWorkflowRequest(String inputKey, String uploadFileId) {
        try {
            return objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                put("inputs", new java.util.HashMap<>() {{
                    put(inputKey, java.util.List.of(new java.util.HashMap<>() {{
                        put("transfer_method", "local_file");  // 使用本地文件传输方式
                        put("upload_file_id", uploadFileId);    // 文件 ID
                        put("type", "document");                // 文件类型为文档
                    }}));
                }});
                put("response_mode", "streaming");  // 流式模式，避免网关超时
                put("user", "iras-user");
            }});
        } catch (Exception e) {
            log.error("构建请求体失败", e);
            throw new RuntimeException("构建请求体失败", e);
        }
    }

    /**
     * 调用 Dify Workflow API 的通用方法。
     * <p>
     * 执行流程：
     * 1. 建立 HTTP 连接并设置请求头
     * 2. 写入 JSON 请求体
     * 3. 读取响应并判断状态码
     * 4. 从响应中提取 AI 推理结果（data.outputs.result）
     * </p>
     *
     * @param urlStr      API 完整 URL
     * @param requestBody JSON 请求体字符串
     * @param apiKey      Dify API Key（Bearer Token）
     * @return AI 生成的结果文本
     * @throws RuntimeException API 调用失败时抛出异常
     */
    /**
     * 解析 Dify SSE 流式响应。
     * <p>
     * Dify streaming 模式返回 Server-Sent Events 格式，关键事件：
     * <ul>
     *   <li>text_chunk - 文本片段，需拼接</li>
     *   <li>workflow_finished - 工作流完成，包含完整 outputs</li>
     * </ul>
     * 优先从 workflow_finished 的 outputs.result 取结果，
     * 若无则返回拼接的 text_chunk 内容。
     * </p>
     *
     * @param inputStream SSE 响应流
     * @return AI 生成的结果文本
     * @throws Exception 读取异常
     */
    private String parseSseStream(java.io.InputStream inputStream) throws Exception {
        StringBuilder textChunks = new StringBuilder();
        String finalResult = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;  // 跳过空行（SSE 事件分隔符）

                // SSE 格式: "data: {json}" 或 "event: xxx"
                if (line.startsWith("data: ")) {
                    String jsonStr = line.substring(6).trim();
                    if ("[DONE]".equals(jsonStr)) break;// Dify 专属结束符，跳出循环

                    try {
                        JsonNode event = objectMapper.readTree(jsonStr);
                        String eventType = event.has("event") ? event.get("event").asText() : "";

                        switch (eventType) {
                            case "text_chunk":
                                // 拼接文本片段
                                if (event.has("data") && event.get("data").has("text")) {
                                    textChunks.append(event.get("data").get("text").asText());
                                }
                                break;

                            case "workflow_finished":
                                // 工作流完成，提取最终结果
                                JsonNode outputs = event.path("data").path("outputs");
                                if (outputs.has("result")) {
                                    finalResult = outputs.get("result").asText();
                                }
                                break;

                            default:
                                // workflow_started, node_started 等事件，忽略
                                break;
                        }
                    } catch (Exception e) {
                        log.debug("SSE 行解析跳过: {}", line);
                    }
                }
            }
        }

        // 优先返回 workflow_finished 的 result，否则返回拼接的文本
        if (finalResult != null) {
            return finalResult;
        }
        if (textChunks.length() > 0) {
            return textChunks.toString();
        }
        throw new RuntimeException("Dify streaming 响应中未获取到结果");
    }

    /** 最大重试次数（针对 504 等暂时性错误） */
    private static final int MAX_RETRIES = 2;

    /** 重试间隔（毫秒） */
    private static final long RETRY_DELAY_MS = 5000;

    /**
     * 调用 Dify Workflow API 的通用方法（含重试机制）。
     * <p>
     * 执行流程：
     * 1. 建立 HTTP 连接并设置请求头
     * 2. 写入 JSON 请求体
     * 3. 读取响应并判断状态码
     * 4. 从响应中提取 AI 推理结果（data.outputs.result）
     * 5. 若遇 502/503/504 等暂时性错误，自动重试最多 {@value MAX_RETRIES} 次
     * </p>
     *
     * @param urlStr      API 完整 URL
     * @param requestBody JSON 请求体字符串
     * @param apiKey      Dify API Key（Bearer Token）
     * @return AI 生成的结果文本
     * @throws RuntimeException API 调用失败时抛出异常
     */
    private String callDifyApi(String urlStr, String requestBody, String apiKey) {
        int attempt = 0;
        while (true) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);  // 设置认证头
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(300000);  // 连接超时 5 分钟（AI 推理耗时较长）
                connection.setReadTimeout(300000);     // 读取超时 5 分钟

                // 写入请求体
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                }

                // 读取响应
                int responseCode = connection.getResponseCode();
                java.io.InputStream inputStream = (responseCode >= 200 && responseCode < 300)
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                if (responseCode >= 200 && responseCode < 300) {
                    // 流式模式：逐行解析 SSE 事件
                    String result = parseSseStream(inputStream);
                    return result;
                }

                // 非 2xx，读取完整错误响应
                String responseBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                log.error("Dify API 响应: code={}, body={}", responseCode, responseBody);

                // 502/503/504 等暂时性错误，可重试
                boolean retryable = (responseCode == 502 || responseCode == 503 || responseCode == 504);
                if (retryable && attempt < MAX_RETRIES) {
                    attempt++;
                    log.warn("Dify API 返回 {}，第 {}/{} 次重试，等待 {}ms...", responseCode, attempt, MAX_RETRIES, RETRY_DELAY_MS);
                    try { Thread.sleep(RETRY_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }

                throw new RuntimeException("Dify API 调用失败: " + responseCode + " - " + responseBody);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                log.error("调用 Dify API 异常", e);
                throw new RuntimeException("调用 Dify API 失败: " + e.getMessage(), e);
            } finally {
                // 释放 HTTP 连接
                if (connection != null) connection.disconnect();
            }
        }
    }
}
