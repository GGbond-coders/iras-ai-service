/**
 * @file DiagnosisRecord.java
 * @description 诊断历史记录实体类。
 *              对应数据库中的 diagnosis_record 表，存储用户的简历诊断结果。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 诊断历史记录实体。
 * <p>
 * 与数据库表 diagnosis_record 一一对应，记录每次简历诊断的完整信息，
 * 用户可在历史记录页面回顾之前的诊断结果。
 * </p>
 */
@Data
public class DiagnosisRecord {

    /** 记录主键 ID（自增） */
    private Long id;

    /** 关联的用户 ID */
    private Long userId;

    /** 上传的简历文件名 */
    private String resumeFilename;

    /** 简历提取的文本内容（用于回顾） */
    private String resumeContent;

    /** 诊断结果 JSON（AI 返回的完整报告） */
    private String diagnosisResult;

    /** AI 推理过程（think 标签内容） */
    private String thinkContent;

    /** 匹配到的岗位数量 */
    private Integer matchCount;

    /** 诊断时间 */
    private LocalDateTime createTime;
}
