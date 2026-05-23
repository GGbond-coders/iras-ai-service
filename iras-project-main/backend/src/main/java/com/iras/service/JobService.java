/**
 * @file JobService.java
 * @description 职位服务接口。
 *              定义职位查询的业务方法签名，包括分页搜索和按 ID 查询。
 *              具体实现由 {@link com.iras.service.impl.JobServiceImpl} 提供。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.service;

import com.iras.dto.JobSearchRequest;
import com.iras.dto.PageResult;
import com.iras.entity.JobInfo;

/**
 * 职位服务接口。
 * <p>
 * 定义职位数据的查询方法：
 * <ul>
 *   <li>searchJobs - 多条件分页搜索职位</li>
 *   <li>getJobById - 根据 ID 获取职位详情</li>
 * </ul>
 * </p>
 */
public interface JobService {

    /**
     * 多条件分页搜索职位。
     *
     * @param request 搜索请求参数（包含搜索条件和分页信息）
     * @return 分页结果，包含职位列表和分页元数据
     */
    PageResult<JobInfo> searchJobs(JobSearchRequest request);

    /**
     * 根据 ID 获取职位详情。
     *
     * @param id 职位 ID
     * @return 职位信息，如果不存在返回 null
     */
    JobInfo getJobById(Long id);
}
