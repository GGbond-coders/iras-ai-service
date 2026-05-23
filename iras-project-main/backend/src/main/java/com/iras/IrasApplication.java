/**
 * @file IrasApplication.java
 * @description IRAS（智能简历诊断系统）Spring Boot 应用程序入口类。
 *              负责启动整个 Spring Boot 应用，并通过 @MapperScan 注解
 *              自动扫描 MyBatis Mapper 接口所在的包路径。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用程序启动类。
 * <p>
 * {@code @SpringBootApplication} 是一个组合注解，包含：
 * <ul>
 *   <li>{@code @Configuration} - 标记为配置类</li>
 *   <li>{@code @EnableAutoConfiguration} - 启用 Spring Boot 自动配置</li>
 *   <li>{@code @ComponentScan} - 启用组件扫描</li>
 * </ul>
 * {@code @MapperScan} 指定 MyBatis Mapper 接口的扫描路径，自动注册 Mapper Bean。
 * </p>
 */
@SpringBootApplication
@MapperScan("com.iras.mapper")  // 扫描 com.iras.mapper 包下的所有 Mapper 接口
public class IrasApplication {

    /**
     * 应用程序主入口方法。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(IrasApplication.class, args);
    }
}
