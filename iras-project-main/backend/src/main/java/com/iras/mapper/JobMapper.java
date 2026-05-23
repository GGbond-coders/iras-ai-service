/**
 * @file JobMapper.java
 * @description 职位数据访问层接口（MyBatis Mapper）。
 *              定义职位表的数据库操作方法，包括多字段模糊搜索、
 *              统计查询、按 ID 查找和查询全部等操作。
 *              复杂 SQL 通过 XML 映射文件实现，简单 SQL 使用注解实现。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.mapper;

import com.iras.entity.JobInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 职位 Mapper 接口。
 * <p>
 * 使用 MyBatis 框架进行数据库访问：
 * <ul>
 *   <li>searchJobs / countSearchJobs - 通过 XML 映射文件实现复杂动态 SQL</li>
 *   <li>findById / findAll - 使用 @Select 注解直接编写简单 SQL</li>
 * </ul>
 * </p>
 */
@Mapper  // 标记为 MyBatis Mapper 接口，由 @MapperScan 自动扫描注册
public interface JobMapper {

    /**
     * 多字段模糊搜索职位（支持分页）。
     * <p>
     * 对应 XML 映射文件中的动态 SQL，支持按职位名称、城市、薪资范围组合搜索。
     * 使用 LIKE 进行模糊匹配，薪资字段需要去除 'k'/'K' 后缀再比较。
     * </p>
     *
     * @param jobName   职位名称（模糊匹配，可为 null）
     * @param city      城市（模糊匹配，可为 null）
     * @param salaryMin 最低薪资（可为 null）
     * @param salaryMax 最高薪资（可为 null）
     * @param offset    分页偏移量（(page-1) * size）
     * @param size      每页记录数
     * @return 符合条件的职位列表
     */
    List<JobInfo> searchJobs(@Param("jobName") String jobName,
                             @Param("city") String city,
                             @Param("salaryMin") String salaryMin,
                             @Param("salaryMax") String salaryMax,
                             @Param("offset") int offset,
                             @Param("size") int size);

    /**
     * 统计搜索结果总数（与 searchJobs 使用相同的搜索条件）。
     * <p>
     * 用于计算分页信息中的总记录数和总页数。
     * </p>
     *
     * @param jobName   职位名称（模糊匹配，可为 null）
     * @param city      城市（模糊匹配，可为 null）
     * @param salaryMin 最低薪资（可为 null）
     * @param salaryMax 最高薪资（可为 null）
     * @return 符合条件的记录总数
     */
    long countSearchJobs(@Param("jobName") String jobName,
                         @Param("city") String city,
                         @Param("salaryMin") String salaryMin,
                         @Param("salaryMax") String salaryMax);

    /**
     * 根据 ID 查询单个职位详情。
     *
     * @param id 职位 ID
     * @return 职位信息，如果不存在返回 null
     */
    @Select("SELECT * FROM job_info WHERE id = #{id}")
    JobInfo findById(Long id);

    /**
     * 获取所有职位列表（供 Dify 诊断匹配用）。
     * <p>
     * 注意：此方法返回全量数据，仅在 AI 诊断时使用，不宜频繁调用。
     * </p>
     *
     * @return 所有职位的列表
     */
    @Select("SELECT * FROM job_info")
    List<JobInfo> findAll();
}
