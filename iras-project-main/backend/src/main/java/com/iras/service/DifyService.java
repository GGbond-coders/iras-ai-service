/**
 * @file DifyService.java
 * @description Dify AI 服务接口。
 *              定义与 Dify AI 平台交互的业务方法签名。
 *              包括职能画像分析和简历智能诊断两个功能。
 *              具体实现由 {@link com.iras.service.impl.DifyServiceImpl} 提供。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Dify AI 服务接口。
 * <p>
 * 封装与 Dify AI 平台的交互逻辑：
 * <ul>
 *   <li>getJobProfile - 调用职能画像 Workflow API</li>
 *   <li>diagnoseResume - 调用简历诊断 Workflow API（支持文件上传）</li>
 * </ul>
 * </p>
 */
public interface DifyService {

    /**
     * 调用 Dify 职能画像 API。
     * <p>
     * 根据职位名称，AI 生成该职位的技能要求、工具清单、经验要求等完整画像。
     * </p>
     *
     * @param jobName 职位名称
     * @return AI 生成的职能画像 JSON 字符串
     */
    String getJobProfile(String jobName);

    /**
     * 调用 Dify 简历诊断 API（文件上传模式）。
     * <p>
     * 将简历文件上传到 Dify 平台，AI 进行深度分析后返回诊断报告。
     * 处理时间较长（约 3 分钟）。
     * </p>
     *
     * @param file 上传的简历文件
     * @return AI 生成的诊断报告 JSON 字符串
     */
    String diagnoseResume(MultipartFile file);
}
