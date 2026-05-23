/**
 * @file AdminService.java
 * @description 管理员服务接口。
 *              定义管理后台的业务方法签名。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.service;

import com.iras.dto.PageResult;
import com.iras.entity.JobInfo;
import com.iras.entity.User;

import java.util.List;
import java.util.Map;

/**
 * 管理员服务接口。
 */
public interface AdminService {

    /** 分页查询用户列表 */
    PageResult<User> getUsers(int page, int size);

    /** 修改用户角色 */
    boolean updateUserRole(Long id, String role);

    /** 删除用户 */
    boolean deleteUser(Long id);

    /** 新增职位 */
    void addJob(JobInfo job);

    /** 更新职位 */
    boolean updateJob(JobInfo job);

    /** 删除职位 */
    boolean deleteJob(Long id);

    /** 分页查询职位列表 */
    PageResult<JobInfo> getJobs(int page, int size);

    /** 获取系统统计数据 */
    Map<String, Object> getStatistics();
}
