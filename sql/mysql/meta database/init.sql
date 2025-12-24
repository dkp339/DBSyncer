DROP DATABASE IF EXISTS `SyncManager`;

CREATE DATABASE `SyncManager`
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `SyncManager`;

-- 系统用户表 (用于登录管理系统)
CREATE TABLE IF NOT EXISTS `app_users` (
    `user_id` INT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(100) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL COMMENT '必须存储加密后的哈希值',
    `role` ENUM('ADMIN', 'USER') NOT NULL DEFAULT 'USER' COMMENT 'ADMIN: 管理员, USER: 普通用户',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 数据源配置表 (用于存储要同步的目标数据库)
CREATE TABLE IF NOT EXISTS `data_source_config` (
    `source_id` INT AUTO_INCREMENT PRIMARY KEY,
    `source_name` VARCHAR(100) NOT NULL UNIQUE COMMENT '例如: MySQL',
    `db_type` VARCHAR(20) NOT NULL COMMENT '数据库类型：MYSQL, ORACLE, POSTGRESQL, SQL_SERVER',
    `host` VARCHAR(255) NOT NULL COMMENT '主机IP或域名',
    `port` INT NOT NULL COMMENT '端口号',
    `db_name` VARCHAR(100) NOT NULL COMMENT '数据库名称',
    `username` VARCHAR(100) NOT NULL COMMENT '用户名',
    `password_encrypted` VARCHAR(512) NOT NULL COMMENT '必须加密存储',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1:启用 0:禁用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0正常 1删除',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 周期性同步任务表
CREATE TABLE IF NOT EXISTS `sync_tasks` (
    `task_id` INT AUTO_INCREMENT PRIMARY KEY,
    `task_name` VARCHAR(100) NOT NULL UNIQUE,
    `cron_expression` VARCHAR(100) NOT NULL COMMENT 'CRON 表达式, 例如 "0 0 2 * * ?" (每天凌晨2点)',
    `source_db_id` INT NOT NULL COMMENT '源数据库 (外键指向 data_source_config)',
    `target_db_id` INT NOT NULL COMMENT '目标数据库 (外键指向 data_source_config)',
    `status` ENUM('ENABLED', 'DISABLED') NOT NULL DEFAULT 'DISABLED',
    `last_run_time` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`source_db_id`) REFERENCES `data_source_config`(`source_id`) ON DELETE CASCADE,
    FOREIGN KEY (`target_db_id`) REFERENCES `data_source_config`(`source_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 同步日志表
CREATE TABLE IF NOT EXISTS `sync_logs` (
    `log_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` INT NULL COMMENT '如果是周期任务, 关联 task_id (外键)',
    `sync_type` ENUM('REAL_TIME', 'PERIODIC', 'MANUAL') NOT NULL COMMENT '同步类型',
    `table_name` VARCHAR(100) NULL COMMENT '实时同步的表名',
    `start_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `end_time` TIMESTAMP NULL,
    `status` ENUM('SUCCESS', 'FAILED', 'CONFLICT') NOT NULL COMMENT '执行状态',
    `records_affected` INT DEFAULT 0,
    `message` TEXT COMMENT '详细日志或错误信息',
    FOREIGN KEY (`task_id`) REFERENCES `sync_tasks`(`task_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 数据冲突表
CREATE TABLE IF NOT EXISTS `data_conflicts` (
    `conflict_id` INT AUTO_INCREMENT PRIMARY KEY,
    `table_name` VARCHAR(100) NOT NULL COMMENT '发生冲突的表名',
    `record_pk` VARCHAR(255) NOT NULL COMMENT '冲突记录的主键值 (用字符串存以兼容复合主键)',
    `conflict_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `status` ENUM('PENDING', 'RESOLVED') NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING: 待处理, RESOLVED: 已解决',
    `source_a_id` INT NOT NULL COMMENT '冲突源A (外键指向 data_source_config)',
    `data_a` JSON NOT NULL COMMENT '来自A库的数据 (JSON格式)',
    `source_b_id` INT NOT NULL COMMENT '冲突源B (外键指向 data_source_config)',
    `data_b` JSON NOT NULL COMMENT '来自B库的数据 (JSON格式)',
    `resolved_by_user_id` INT NULL COMMENT '解决该冲突的管理员 (外键)',
    `resolved_at` TIMESTAMP NULL,
    `resolution_choice` ENUM('A', 'B', 'MERGE') NULL COMMENT '管理员的选择',
    FOREIGN KEY (`source_a_id`) REFERENCES `data_source_config`(`source_id`) ON DELETE CASCADE,
    FOREIGN KEY (`source_b_id`) REFERENCES `data_source_config`(`source_id`) ON DELETE CASCADE,
    FOREIGN KEY (`resolved_by_user_id`) REFERENCES `app_users`(`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 系统配置表
CREATE TABLE IF NOT EXISTS `sys_config` (
    config_key VARCHAR(50) PRIMARY KEY,
    config_value VARCHAR(255),
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


INSERT IGNORE INTO `sys_config` (config_key, config_value, description)
VALUES ('sync.cron', '0 0 1 * * ?', '周期同步Cron表达式');

INSERT IGNORE INTO `app_users` (`username`, `password_hash`, `role`)
VALUES ('admin', '$2b$10$UvpcWokPJdlAgHXYfEX2fO8pIxL6VdN8hDuGSzeFXY2J1PbEunTdW', 'ADMIN');

INSERT IGNORE INTO `app_users` (`username`, `password_hash`, `role`)
VALUES ('user', '$2b$10$UvpcWokPJdlAgHXYfEX2fO8pIxL6VdN8hDuGSzeFXY2J1PbEunTdW', 'USER');

INSERT INTO data_source_config
(source_name, db_type, host, port, db_name, username, password_encrypted, status, deleted)
VALUES
-- MySQL 数据源
('MySQL-virtualcampus','MYSQL','127.0.0.1',3307,'VirtualCampus','root','HUD/ywPbjgYX5aKhQjL4htDYtjC4e1wXpQKB8URg2N6y9Q==',1,0),

-- PostgreSQL 数据源
('Postgre-virtualcampus','POSTGRESQL','127.0.0.1',5432,'virtualcampus','root','He58Pp1okg6UJjIHF4h2OzFgM2kHl0+ArYaqNh1IznVINA==',1,0),

-- Oracle 数据源
('Oracle-virtualcampus','ORACLE','127.0.0.1',1521,'XEPDB1','root','/uJMrN8RDyOvSudd87OyXOTO82QHinecsWSwlqstXzD1pQ==',1,0);



-- 脚本结束
SELECT '元数据库 (SyncManager) 初始化完成。' AS `Status`;