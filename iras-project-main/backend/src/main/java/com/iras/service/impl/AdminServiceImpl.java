/**
 * @file AdminServiceImpl.java
 * @description 管理员服务实现类。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.service.impl;

import com.iras.dto.PageResult;
import com.iras.entity.JobInfo;
import com.iras.entity.User;
import com.iras.mapper.AdminMapper;
import com.iras.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;

    @Override
    public PageResult<User> getUsers(int page, int size) {
        int offset = (page - 1) * size;
        List<User> records = adminMapper.findAllUsers(offset, size);
        long total = adminMapper.countUsers();

        PageResult<User> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setTotalPages((int) Math.ceil((double) total / size));
        return result;
    }

    @Override
    public boolean updateUserRole(Long id, String role) {
        return adminMapper.updateUserRole(id, role) > 0;
    }

    @Override
    public boolean deleteUser(Long id) {
        return adminMapper.deleteUser(id) > 0;
    }

    @Override
    public void addJob(JobInfo job) {
        adminMapper.insertJob(job);
    }

    @Override
    public boolean updateJob(JobInfo job) {
        return adminMapper.updateJob(job) > 0;
    }

    @Override
    public boolean deleteJob(Long id) {
        return adminMapper.deleteJob(id) > 0;
    }

    @Override
    public PageResult<JobInfo> getJobs(int page, int size) {
        int offset = (page - 1) * size;
        List<JobInfo> records = adminMapper.findAllJobs(offset, size);
        long total = adminMapper.countJobs();

        PageResult<JobInfo> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setTotalPages((int) Math.ceil((double) total / size));
        return result;
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", adminMapper.countUsers());         // 用户总数
        stats.put("jobCount", adminMapper.countJobs());           // 职位总数
        stats.put("diagnosisCount", adminMapper.countDiagnoses()); // 诊断总数
        stats.put("userTrend", adminMapper.getUserTrend());       // 近 7 天注册趋势
        stats.put("diagnosisTrend", adminMapper.getDiagnosisTrend()); // 近 7 天诊断趋势
        stats.put("cityDistribution", adminMapper.getJobCityDistribution()); // 城市分布
        return stats;
    }
}
