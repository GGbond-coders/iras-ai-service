-- ==============================================================================
-- IRAS 智能简历诊断系统 - 数据库增量更新脚本
-- ==============================================================================
-- 新增功能：
-- 1. 诊断历史记录表（diagnosis_record）
-- 2. 用户表增加角色字段（role）
-- ==============================================================================

USE `iras`;

-- ==============================================================================
-- 1. 用户表增加角色字段（普通用户 / 管理员）
-- ==============================================================================
ALTER TABLE `user` ADD COLUMN `role` varchar(20) NOT NULL DEFAULT 'user' COMMENT '用户角色: user-普通用户, admin-管理员' AFTER `email`;

-- 将第一个注册的用户设为管理员（方便初始登录）
UPDATE `user` SET `role` = 'admin' WHERE `id` = 1;

-- ==============================================================================
-- 2. 诊断历史记录表 - 存储用户的简历诊断结果
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
