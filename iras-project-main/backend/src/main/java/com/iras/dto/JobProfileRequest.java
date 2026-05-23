/**
 * @file JobProfileRequest.java
 * @description 职能画像请求 DTO（数据传输对象）。
 *              封装调用 Dify 职能画像接口所需的参数。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.dto;

import lombok.Data;

/**
 * 职能画像请求数据传输对象。
 * <p>
 * 用于向 Dify AI 平台发起职能画像分析请求。
 * 只需提供职位名称，AI 将生成该职位的完整画像。
 * </p>
 */
@Data
public class JobProfileRequest {

    /** 职位名称，如 "软件工程师"、"产品经理" 等 */
    private String jobName;
}
