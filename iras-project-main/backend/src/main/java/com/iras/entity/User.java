/**
 * @file User.java
 * @description 用户实体类。
 *              对应数据库中的 user 表，封装用户的基本信息。
 *              密码字段存储的是 BCrypt 加密后的哈希值。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体。
 * <p>
 * 与数据库表 user 一一对应：
 * <ul>
 *   <li>id -> id（主键，自增）</li>
 *   <li>username -> username（用户名，唯一）</li>
 *   <li>password -> password（BCrypt 加密后的密码）</li>
 *   <li>email -> email（联系邮箱，可选）</li>
 *   <li>createTime -> create_time（注册时间）</li>
 * </ul>
 * </p>
 */
@Data
public class User {

    /** 用户主键 ID（自增） */
    private Long id;

    /** 用户名（唯一约束） */
    private String username;

    /** 密码（BCrypt 加密存储，不返回给前端） */
    private String password;

    /** 联系邮箱（可选） */
    private String email;

    /** 用户角色（user-普通用户, admin-管理员） */
    private String role;

    /** 用户注册时间 */
    private LocalDateTime createTime;
}
