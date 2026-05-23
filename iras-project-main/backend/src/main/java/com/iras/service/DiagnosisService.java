/**
 * @file DiagnosisService.java
 * @description 诊断历史记录服务接口。
 *              定义诊断记录的业务方法签名。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.service;

import com.iras.dto.PageResult;
import com.iras.entity.DiagnosisRecord;

/**
 * 诊断历史记录服务接口。
 */
public interface DiagnosisService {

    /**
     * 保存诊断记录。
     *
     * @param record 诊断记录
     */
    void saveRecord(DiagnosisRecord record);

    /**
     * 分页查询用户的诊断历史。
     *
     * @param userId 用户 ID
     * @param page   页码
     * @param size   每页记录数
     * @return 分页结果
     */
    PageResult<DiagnosisRecord> getHistory(Long userId, int page, int size);

    /**
     * 查询诊断记录详情。
     *
     * @param id     记录 ID
     * @param userId 用户 ID（校验归属）
     * @return 诊断记录详情
     */
    DiagnosisRecord getDetail(Long id, Long userId);

    /**
     * 删除诊断记录。
     *
     * @param id     记录 ID
     * @param userId 用户 ID（校验归属）
     * @return 是否删除成功
     */
    boolean deleteRecord(Long id, Long userId);
}
