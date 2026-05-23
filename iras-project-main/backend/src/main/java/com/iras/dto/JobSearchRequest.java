/**
 * @file JobSearchRequest.java
 * @description 职位搜索请求 DTO（数据传输对象）。
 *              封装职位搜索的查询条件和分页参数。
 *              所有搜索条件均为可选项，支持多字段组合模糊搜索。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.dto;

import lombok.Data;

/**
 * 职位搜索请求数据传输对象。
 * <p>
 * 支持的搜索条件：
 * <ul>
 *   <li>jobName - 职位名称（模糊匹配）</li>
 *   <li>city - 工作城市（模糊匹配）</li>
 *   <li>salaryMin - 最低薪资</li>
 *   <li>salaryMax - 最高薪资</li>
 * </ul>
 * 分页参数：
 * <ul>
 *   <li>page - 页码，默认为 1</li>
 *   <li>size - 每页条数，默认为 20</li>
 * </ul>
 * </p>
 */
@Data
public class JobSearchRequest {

    /** 职位名称搜索条件（模糊匹配） */
    private String jobName;

    /** 工作城市搜索条件（模糊匹配） */
    private String city;

    /** 最低薪资筛选条件 */
    private String salaryMin;

    /** 最高薪资筛选条件 */
    private String salaryMax;

    /** 页码，默认第 1 页 */
    private Integer page = 1;

    /** 每页记录数，默认 20 条 */
    private Integer size = 20;
}
