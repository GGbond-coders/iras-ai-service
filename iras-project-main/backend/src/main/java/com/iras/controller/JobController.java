/**
 * @file JobController.java
 * @description 职位控制器。
 *              提供职位搜索和职位详情查询的 REST API 接口。
 *              支持多字段模糊搜索和分页查询。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.controller;

import com.iras.dto.JobSearchRequest;
import com.iras.dto.PageResult;
import com.iras.dto.Result;
import com.iras.entity.JobInfo;
import com.iras.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 职位控制器。
 * <p>
 * 提供职位数据的查询接口：
 * <ul>
 *   <li>职位搜索（GET /api/jobs/search）- 支持按名称、城市、薪资范围搜索，支持分页</li>
 *   <li>职位详情（GET /api/jobs/{id}）- 根据 ID 获取单个职位详情</li>
 * </ul>
 * </p>
 */
@RestController                // 声明为 REST 控制器
@RequestMapping("/api/jobs")   // 映射请求路径前缀
@RequiredArgsConstructor
public class JobController {

    /** 职位服务，处理职位查询的业务逻辑 */
    private final JobService jobService;

    /**
     * 职位搜索接口 - 多字段模糊匹配 + 分页。
     * <p>
     * 支持的搜索条件：
     * <ul>
     *   <li>jobName - 职位名称（模糊匹配）</li>
     *   <li>city - 工作城市（模糊匹配）</li>
     *   <li>salaryMin - 最低薪资</li>
     *   <li>salaryMax - 最高薪资</li>
     *   <li>page - 页码（默认 1）</li>
     *   <li>size - 每页条数（默认 20）</li>
     * </ul>
     * </p>
     *
     * @param request 职位搜索请求对象，包含搜索条件和分页参数
     * @return 统一响应结果，包含分页后的职位列表和总数信息
     */
    @GetMapping("/search")
    public Result<PageResult<JobInfo>> searchJobs(JobSearchRequest request) {
        // 调用服务层执行搜索逻辑
        PageResult<JobInfo> result = jobService.searchJobs(request);
        return Result.success(result);
    }

    /**
     * 获取职位详情接口。
     * <p>
     * 根据职位 ID 查询单个职位的完整信息。
     * 如果职位不存在，返回 404 错误。
     * </p>
     *
     * @param id 职位 ID（路径参数）
     * @return 统一响应结果，包含职位详情信息
     */
    @GetMapping("/{id}")
    public Result<JobInfo> getJobById(@PathVariable Long id) {
        // 根据 ID 查询职位
        JobInfo job = jobService.getJobById(id);
        // 判断职位是否存在
        if (job == null) {
            return Result.error(404, "职位不存在");
        }
        return Result.success(job);
    }
}
