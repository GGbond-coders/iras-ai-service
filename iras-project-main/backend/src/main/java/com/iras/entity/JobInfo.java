/**
 * @file JobInfo.java
 * @description 职位信息实体类。
 *              对应数据库中的 job_info 表，封装职位的基本信息，
 *              包括职位名称、公司名称、城市、薪资、JD 内容等字段。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 职位信息实体。
 * <p>
 * 与数据库表 job_info 一一对应，字段映射关系：
 * <ul>
 *   <li>id -> id（主键）</li>
 *   <li>jobName -> job_name（职位名称）</li>
 *   <li>companyName -> company_name（公司名称）</li>
 *   <li>city -> city（工作城市）</li>
 *   <li>salary -> salary（薪资范围）</li>
 *   <li>jdText -> jd_text（职位描述/JD 内容）</li>
 *   <li>type -> type（职位类型，如全职/实习/校招）</li>
 *   <li>createTime -> create_time（创建时间）</li>
 *   <li>updateTime -> update_time（更新时间）</li>
 * </ul>
 * 注意：驼峰命名会通过 MyBatis 配置自动映射到下划线命名的数据库字段。
 * </p>
 */
@Data  // Lombok 自动生成 getter/setter/toString/equals/hashCode
public class JobInfo {

    /** 职位主键 ID */
    private Long id;

    /** 职位名称，如 "Java后台开发工程师" */
    private String jobName;

    /** 公司名称 */
    private String companyName;

    /** 工作城市，如 "上海"、"北京" */
    private String city;

    /** 薪资范围，如 "15000"、"20000" */
    private String salary;

    /** 职位描述内容（Job Description），包含技能要求、福利等信息 */
    private String jdText;

    /** 职位类型，如 "全职"、"实习"、"校招" */
    private String type;

    /** 数据创建时间 */
    private LocalDateTime createTime;

    /** 数据最后更新时间 */
    private LocalDateTime updateTime;
}
