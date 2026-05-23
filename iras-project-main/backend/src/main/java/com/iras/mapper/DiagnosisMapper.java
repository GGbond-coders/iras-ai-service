/**
 * @file DiagnosisMapper.java
 * @description 诊断历史记录数据访问层接口（MyBatis Mapper）。
 *              定义诊断记录的数据库操作方法，包括插入、查询和删除。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.mapper;

import com.iras.entity.DiagnosisRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 诊断历史记录 Mapper 接口。
 * <p>
 * 提供诊断记录的 CRUD 操作：
 * <ul>
 *   <li>insert - 插入新的诊断记录</li>
 *   <li>findByUserId - 分页查询用户的诊断历史</li>
 *   <li>countByUserId - 统计用户的诊断记录总数</li>
 *   <li>findById - 根据 ID 查询单条记录详情</li>
 *   <li>deleteById - 删除指定记录</li>
 * </ul>
 * </p>
 */
@Mapper
public interface DiagnosisMapper {

    /**
     * 插入新的诊断记录。
     *
     * @param record 诊断记录对象
     * @return 受影响的行数
     */
    @Insert("INSERT INTO diagnosis_record (user_id, resume_filename, resume_content, diagnosis_result, think_content, match_count, create_time) " +
            "VALUES (#{userId}, #{resumeFilename}, #{resumeContent}, #{diagnosisResult}, #{thinkContent}, #{matchCount}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DiagnosisRecord record);

    /**
     * 分页查询用户的诊断历史（按时间倒序）。
     *
     * @param userId 用户 ID
     * @param offset 分页偏移量
     * @param size   每页记录数
     * @return 诊断记录列表
     */
    @Select("SELECT id, user_id, resume_filename, match_count, create_time " +
            "FROM diagnosis_record WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<DiagnosisRecord> findByUserId(@Param("userId") Long userId,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    /**
     * 统计用户的诊断记录总数。
     *
     * @param userId 用户 ID
     * @return 记录总数
     */
    @Select("SELECT COUNT(*) FROM diagnosis_record WHERE user_id = #{userId}")
    long countByUserId(Long userId);

    /**
     * 根据 ID 查询单条诊断记录详情（包含完整诊断结果）。
     *
     * @param id 记录 ID
     * @return 诊断记录详情
     */
    @Select("SELECT * FROM diagnosis_record WHERE id = #{id}")
    DiagnosisRecord findById(Long id);

    /**
     * 删除指定诊断记录。
     *
     * @param id 记录 ID
     * @return 受影响的行数
     */
    @Delete("DELETE FROM diagnosis_record WHERE id = #{id}")
    int deleteById(Long id);
}
