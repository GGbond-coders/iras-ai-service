/**
 * @file UserMapper.java
 * @description 用户数据访问层接口（MyBatis Mapper）。
 *              定义用户表的数据库操作方法，包括按用户名/邮箱/ID 查询用户
 *              以及插入新用户等操作。所有 SQL 均通过注解方式实现。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.mapper;

import com.iras.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper 接口。
 * <p>
 * 提供用户表的基本 CRUD 操作：
 * <ul>
 *   <li>findByUsername - 按用户名查询（用于登录验证和重名检查）</li>
 *   <li>findByEmail - 按邮箱查询（用于邮箱登录和重复检查）</li>
 *   <li>findById - 按 ID 查询</li>
 *   <li>insert - 插入新用户记录</li>
 * </ul>
 * </p>
 */
@Mapper
public interface UserMapper {

    /**
     * 根据用户名查询用户。
     *
     * @param username 用户名
     * @return 用户信息，如果不存在返回 null
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    /**
     * 根据邮箱查询用户。
     *
     * @param email 邮箱地址
     * @return 用户信息，如果不存在返回 null
     */
    @Select("SELECT * FROM user WHERE email = #{email}")
    User findByEmail(String email);

    /**
     * 根据用户 ID 查询用户。
     *
     * @param id 用户 ID
     * @return 用户信息，如果不存在返回 null
     */
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Long id);

    /**
     * 插入新用户记录。
     * <p>
     * 使用 {@code @Options(useGeneratedKeys = true)} 获取数据库自增主键，
     * 插入后自动将生成的 ID 回填到 User 对象的 id 字段。
     * </p>
     *
     * @param user 用户信息对象
     * @return 受影响的行数（1 表示插入成功）
     */
    @Insert("INSERT INTO user (username, password, email) VALUES (#{username}, #{password}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
}
