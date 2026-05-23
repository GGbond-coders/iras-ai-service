-- ==============================================================================
-- IRAS 智能简历诊断系统 - 数据库初始化脚本
-- ==============================================================================
-- 此脚本用于初始化 IRAS 系统的数据库，包括：
-- 1. 创建数据库（使用 utf8mb4 字符集）
-- 2. 创建用户表（user）- 存储系统用户信息
-- 3. 创建职位库表（job_info）- 存储职位数据
-- 4. 插入示例职位数据
--
-- 执行方式：mysql -u root -p < init.sql
-- ==============================================================================

-- 创建数据库（如果不存在），使用 utf8mb4 字符集以支持 emoji 和特殊字符
CREATE DATABASE IF NOT EXISTS `iras` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `iras`;

-- ==============================================================================
-- 1. 用户表 - 存储系统注册用户的基本信息
-- ==============================================================================
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,                    -- 用户主键 ID（自增）
  `username` varchar(50) NOT NULL COMMENT '用户名',        -- 用户名（唯一约束）
  `password` varchar(255) NOT NULL COMMENT 'BCrypt加密密码', -- BCrypt 加密后的密码
  `email` varchar(100) DEFAULT NULL COMMENT '联系邮箱',    -- 联系邮箱（可选）
  `role` varchar(20) NOT NULL DEFAULT 'user' COMMENT '用户角色: user-普通用户, admin-管理员', -- 用户角色
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', -- 注册时间（自动填充）
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)                   -- 用户名唯一索引，防止重复注册
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户系统表';

-- ==============================================================================
-- 2. 职位库表 - 存储职位信息，支持全文检索和多字段搜索
-- ==============================================================================
CREATE TABLE IF NOT EXISTS `job_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',     -- 职位主键 ID（自增）
  `job_name` varchar(100) NOT NULL COMMENT '职位名称',      -- 职位名称
  `company_name` varchar(100) NOT NULL COMMENT '公司名称',   -- 招聘公司名称
  `city` varchar(50) DEFAULT NULL COMMENT '城市',           -- 工作城市
  `salary` varchar(50) DEFAULT NULL COMMENT '薪资范围',     -- 薪资范围（如 "15000"）
  `jd_text` text NOT NULL COMMENT '职位详情(JD内容)',       -- 职位描述全文（用于搜索和 AI 分析）
  `type` varchar(50) DEFAULT NULL COMMENT '职位分类(如: 全职/实习/校招)', -- 职位类型
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '数据导入时间', -- 数据创建时间
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', -- 最后更新时间（自动更新）
  PRIMARY KEY (`id`),
  KEY `idx_job_name` (`job_name`),                         -- 职位名称索引（加速模糊查询）
  KEY `idx_city` (`city`),                                 -- 城市索引（加速城市筛选）
  FULLTEXT KEY `idx_fulltext_jd` (`jd_text`)               -- JD 内容全文索引（支持全文检索）
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位库表';

-- ==============================================================================
-- 3. 插入示例职位数据（覆盖多种技术栈和城市）
-- ==============================================================================
INSERT INTO `job_info` (`job_name`, `company_name`, `city`, `salary`, `jd_text`, `type`) VALUES
('Java后台开发工程师', '上海联泉智能科技有限公司', '上海', '15000', '5年及以上 本科 Java 后台开发 java 软件开发 软件工程师 开发 java开发 开发工程师 软件 工程师 五险一金 年终奖金 员工旅游 绩效奖金 餐饮补贴 交通补贴 通讯补贴', '全职'),
('前端开发工程师', '北京字节跳动科技有限公司', '北京', '20000', '3年及以上 本科 前端开发 JavaScript TypeScript Vue React HTML CSS Web开发 五险一金 弹性工作 免费三餐', '全职'),
('Python测试开发工程师', '深圳腾讯计算机系统有限公司', '深圳', '18000', '3年及以上 本科 Python 测试开发 自动化测试 性能测试 Selenium pytest 接口测试 五险一金 股票期权', '全职'),
('Android开发工程师', '杭州阿里巴巴网络科技有限公司', '杭州', '22000', '3年及以上 本科 Android Java Kotlin 移动开发 MVVM Jetpack 五险一金 补充医疗 带薪年假', '全职'),
('iOS开发工程师', '北京小米科技有限公司', '北京', '20000', '3年及以上 本科 iOS Swift Objective-C 移动开发 UIKit SwiftUI 五险一金 年终奖金', '全职'),
('后端开发工程师', '上海拼多多信息技术有限公司', '上海', '25000', '5年及以上 本科 Java Go 微服务 分布式 高并发 Spring Cloud Kubernetes 五险一金 股票期权', '全职'),
('数据分析师', '广州网易计算机系统有限公司', '广州', '15000', '2年及以上 本科 SQL Python 数据分析 Tableau 数据可视化 统计学 五险一金 年终奖金', '全职'),
('算法工程师', '北京百度网讯科技有限公司', '北京', '30000', '3年及以上 硕士 机器学习 深度学习 NLP CV Python TensorFlow PyTorch 五险一金 股票期权', '全职');

-- ==============================================================================
-- 4. 诊断历史记录表 - 存储用户的简历诊断结果
-- ==============================================================================
CREATE TABLE IF NOT EXISTS `diagnosis_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '关联用户ID',
  `resume_filename` varchar(255) NOT NULL COMMENT '简历文件名',
  `resume_content` mediumtext COMMENT '简历提取的文本内容（用于回顾）',
  `diagnosis_result` mediumtext COMMENT '诊断结果JSON（AI返回的完整报告）',
  `think_content` mediumtext COMMENT 'AI推理过程（think标签内容）',
  `match_count` int DEFAULT 0 COMMENT '匹配到的岗位数量',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '诊断时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_diagnosis_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='诊断历史记录表';
