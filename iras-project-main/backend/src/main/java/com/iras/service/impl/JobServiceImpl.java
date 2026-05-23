/**
 * @file JobServiceImpl.java
 * @description 职位服务实现类。
 *              实现职位分页搜索和按 ID 查询的业务逻辑。
 *              负责处理分页参数的默认值、调用 Mapper 层查询数据、
 *              以及组装分页结果对象。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.service.impl;

import com.iras.dto.JobSearchRequest;
import com.iras.dto.PageResult;
import com.iras.entity.JobInfo;
import com.iras.mapper.JobMapper;
import com.iras.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 职位服务实现类。
 * <p>
 * 核心职责：
 * <ul>
 *   <li>处理分页参数（页码、每页大小、偏移量）</li>
 *   <li>调用 Mapper 执行数据库查询</li>
 *   <li>计算总页数并组装分页结果</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    /** 职位数据访问层 */
    private final JobMapper jobMapper;

    /**
     * 多条件分页搜索职位。
     * <p>
     * 处理逻辑：
     * 1. 设置分页参数默认值（page 默认 1，size 默认 20）
     * 2. 计算分页偏移量 offset = (page - 1) * size
     * 3. 查询当前页数据列表
     * 4. 查询符合条件的总记录数
     * 5. 计算总页数并组装 PageResult 返回
     * </p>
     *
     * @param request 搜索请求参数
     * @return 分页结果对象
     */
    @Override
    public PageResult<JobInfo> searchJobs(JobSearchRequest request) {
        // 处理分页参数默认值
        int page = request.getPage() != null ? request.getPage() : 1;
        int size = request.getSize() != null ? request.getSize() : 20;
        // 计算 SQL 分页偏移量
        int offset = (page - 1) * size;

        // 查询当前页的职位列表
        List<JobInfo> records = jobMapper.searchJobs(
                request.getJobName(), request.getCity(),
                request.getSalaryMin(), request.getSalaryMax(),
                offset, size);

        // 查询符合条件的总记录数（用于分页计算）
        long total = jobMapper.countSearchJobs(
                request.getJobName(), request.getCity(),
                request.getSalaryMin(), request.getSalaryMax());

        // 组装分页结果对象
        PageResult<JobInfo> result = new PageResult<>();
        result.setRecords(records);    // 当前页数据
        result.setTotal(total);        // 总记录数
        result.setPage(page);          // 当前页码
        result.setSize(size);          // 每页大小
        result.setTotalPages((int) Math.ceil((double) total / size));  // 总页数（向上取整）

        return result;
    }

    /**
     * 根据 ID 获取职位详情。
     *
     * @param id 职位 ID
     * @return 职位信息，如果不存在返回 null
     */
    @Override
    public JobInfo getJobById(Long id) {
        return jobMapper.findById(id);
    }
}
