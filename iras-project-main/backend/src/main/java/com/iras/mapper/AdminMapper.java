/**
 * @file AdminMapper.java
 * @description 管理员数据访问层接口（MyBatis Mapper）。
 *              提供管理后台所需的数据库操作，包括用户管理、
 *              职位管理和系统统计数据查询。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.mapper;

import com.iras.entity.JobInfo;
import com.iras.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员 Mapper 接口。
 * <p>
 * 提供管理后台的数据操作：
 * <ul>
 *   <li>用户管理：查询全部用户、修改角色、删除用户</li>
 *   <li>职位管理：插入、更新、删除职位</li>
 *   <li>系统统计：用户总数、职位总数、诊断总数、近期趋势</li>
 * </ul>
 * </p>
 */
@Mapper
public interface AdminMapper {

    // ==================== 用户管理 ====================

    /**
     * 分页查询全部用户列表。
     *
     * @param offset 分页偏移量
     * @param size   每页记录数
     * @return 用户列表（不含密码字段）
     */
    @Select("SELECT id, username, email, role, create_time FROM user ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<User> findAllUsers(@Param("offset") int offset, @Param("size") int size);

    /**
     * 统计用户总数。
     *
     * @return 用户总数
     */
    @Select("SELECT COUNT(*) FROM user")
    long countUsers();

    /**
     * 修改用户角色。
     *
     * @param id   用户 ID
     * @param role 新角色（user / admin）
     * @return 受影响的行数
     */
    @Update("UPDATE user SET role = #{role} WHERE id = #{id}")
    int updateUserRole(@Param("id") Long id, @Param("role") String role);

    /**
     * 删除用户。
     *
     * @param id 用户 ID
     * @return 受影响的行数
     */
    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteUser(Long id);

    // ==================== 职位管理 ====================

    /**
     * 插入新职位。
     *
     * @param job 职位信息对象
     * @return 受影响的行数
     */
    @Insert("INSERT INTO job_info (job_name, company_name, city, salary, jd_text, type, status, create_time, update_time) " +
            "VALUES (#{jobName}, #{companyName}, #{city}, #{salary}, #{jdText}, #{type}, 'active', NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertJob(JobInfo job);

    /**
     * 更新职位信息。
     *
     * @param job 职位信息对象
     * @return 受影响的行数
     */
    @Update("UPDATE job_info SET job_name=#{jobName}, company_name=#{companyName}, city=#{city}, " +
            "salary=#{salary}, jd_text=#{jdText}, type=#{type}, update_time=NOW() WHERE id=#{id}")
    int updateJob(JobInfo job);

    /**
     * 删除职位。
     *
     * @param id 职位 ID
     * @return 受影响的行数
     */
    @Delete("DELETE FROM job_info WHERE id = #{id}")
    int deleteJob(Long id);

    /**
     * 分页查询全部职位（管理用）。
     *
     * @param offset 分页偏移量
     * @param size   每页记录数
     * @return 职位列表
     */
    @Select("SELECT * FROM job_info ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<JobInfo> findAllJobs(@Param("offset") int offset, @Param("size") int size);

    /**
     * 统计职位总数。
     *
     * @return 职位总数
     */
    @Select("SELECT COUNT(*) FROM job_info")
    long countJobs();

    // ==================== 系统统计 ====================

    /**
     * 统计诊断记录总数。
     *
     * @return 诊断记录总数
     */
    @Select("SELECT COUNT(*) FROM diagnosis_record")
    long countDiagnoses();

    /**
     * 获取近 7 天每日注册用户数。
     *
     * @return 日期-数量列表
     */
    @Select("SELECT DATE(create_time) AS date, COUNT(*) AS count FROM user " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> getUserTrend();

    /**
     * 获取近 7 天每日诊断次数。
     *
     * @return 日期-数量列表
     */
    @Select("SELECT DATE(create_time) AS date, COUNT(*) AS count FROM diagnosis_record " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> getDiagnosisTrend();

    /**
     * 获取职位城市分布统计（Top 10）。
     *
     * @return 城市-数量列表
     */
    @Select("SELECT city, COUNT(*) AS count FROM job_info WHERE city IS NOT NULL AND city != '' " +
            "GROUP BY city ORDER BY count DESC LIMIT 10")
    List<Map<String, Object>> getJobCityDistribution();
}
