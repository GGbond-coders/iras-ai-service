/**
 * @file Result.java
 * @description 统一响应结果封装类。
 *              所有 REST API 接口统一使用此格式返回数据，
 *              包含状态码（code）、提示消息（message）和数据体（data）。
 *              使用泛型 T 支持任意类型的返回数据。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应结果封装。
 * <p>
 * 泛型参数 T 表示返回数据的类型。
 * 提供静态工厂方法快速创建成功/失败响应：
 * <ul>
 *   <li>{@link #success(Object)} - 创建成功响应（code=200）</li>
 *   <li>{@link #success(String, Object)} - 创建带自定义消息的成功响应</li>
 *   <li>{@link #error(int, String)} - 创建指定错误码的失败响应</li>
 *   <li>{@link #error(String)} - 创建默认 500 错误码的失败响应</li>
 * </ul>
 * </p>
 *
 * @param <T> 响应数据的类型
 */
@Data                // Lombok 自动生成 getter/setter/toString/equals/hashCode
@AllArgsConstructor  // Lombok 生成全参构造函数
@NoArgsConstructor   // Lombok 生成无参构造函数
public class Result<T> {

    /** 响应状态码，200 表示成功，其他为错误码 */
    private int code;

    /** 响应提示消息 */
    private String message;

    /** 响应数据体 */
    private T data;

    /**
     * 创建成功响应（默认消息 "success"）。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功的 Result 对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 创建带自定义消息的成功响应。
     *
     * @param message 自定义成功消息
     * @param data    响应数据
     * @param <T>     数据类型
     * @return 成功的 Result 对象
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 创建失败响应（data 为 null）。
     *
     * @param code    错误状态码
     * @param message 错误提示消息
     * @param <T>     数据类型
     * @return 失败的 Result 对象
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 创建默认 500 错误码的失败响应。
     *
     * @param message 错误提示消息
     * @param <T>     数据类型
     * @return 失败的 Result 对象（code=500）
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
}
