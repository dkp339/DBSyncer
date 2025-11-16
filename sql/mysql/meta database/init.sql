CREATE DATABASE IF NOT EXISTS `SyncManager`
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
CREATE TABLE IF NOT EXISTS `data_sources` (
    `source_id` INT AUTO_INCREMENT PRIMARY KEY,
    `source_name` VARCHAR(100) NOT NULL UNIQUE COMMENT '例如: MySQL',
    `db_type` ENUM('MySQL', 'Oracle', 'SQL Server', 'PostgreSQL') NOT NULL COMMENT '数据库类型',
    `host` VARCHAR(255) NOT NULL COMMENT '主机IP或域名',
    `port` INT NOT NULL COMMENT '端口号',
    `db_name` VARCHAR(100) NOT NULL COMMENT '数据库名称',
    `username` VARCHAR(100) NOT NULL COMMENT '用户名',
    `password_encrypted` VARCHAR(512) NOT NULL COMMENT '必须由应用程序加密存储',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `last_test_status` ENUM('SUCCESS', 'FAILED', 'PENDING') DEFAULT 'PENDING',
    `last_test_message` TEXT
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 周期性同步任务表
CREATE TABLE IF NOT EXISTS `sync_tasks` (
    `task_id` INT AUTO_INCREMENT PRIMARY KEY,
    `task_name` VARCHAR(100) NOT NULL UNIQUE,
    `cron_expression` VARCHAR(100) NOT NULL COMMENT 'CRON 表达式, 例如 "0 0 2 * * ?" (每天凌晨2点)',
    `source_db_id` INT NOT NULL COMMENT '源数据库 (外键指向 data_sources)',
    `target_db_id` INT NOT NULL COMMENT '目标数据库 (外键指向 data_sources)',
    `status` ENUM('ENABLED', 'DISABLED') NOT NULL DEFAULT 'DISABLED',
    `last_run_time` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`source_db_id`) REFERENCES `data_sources`(`source_id`) ON DELETE CASCADE,
    FOREIGN KEY (`target_db_id`) REFERENCES `data_sources`(`source_id`) ON DELETE CASCADE
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
    `source_a_id` INT NOT NULL COMMENT '冲突源A (外键指向 data_sources)',
    `data_a` JSON NOT NULL COMMENT '来自A库的数据 (JSON格式)',
    `source_b_id` INT NOT NULL COMMENT '冲突源B (外键指向 data_sources)',
    `data_b` JSON NOT NULL COMMENT '来自B库的数据 (JSON格式)',
    `resolved_by_user_id` INT NULL COMMENT '解决该冲突的管理员 (外键)',
    `resolved_at` TIMESTAMP NULL,
    `resolution_choice` ENUM('A', 'B', 'MERGE') NULL COMMENT '管理员的选择',
    FOREIGN KEY (`source_a_id`) REFERENCES `data_sources`(`source_id`) ON DELETE CASCADE,
    FOREIGN KEY (`source_b_id`) REFERENCES `data_sources`(`source_id`) ON DELETE CASCADE,
    FOREIGN KEY (`resolved_by_user_id`) REFERENCES `app_users`(`user_id`) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- 8. 插入一个默认的管理员账户 (密码: 123456)
-- 使用 INSERT IGNORE 来确保脚本可重复运行 (如果 username 已存在, 则忽略此条)
-- 密码 '123456' 的 Bcrypt 哈希值 (成本因子=10)
INSERT IGNORE INTO `app_users` (`username`, `password_hash`, `role`)
VALUES
    ('admin', '$2a$10$f.2.G.b.H.I.j.k.l.m.n.o.p.q.r.s.t.u.v.w.x.y.z.A.B.C', 'ADMIN');
-- 备注: 上面的哈希值 $2a$10$... 是 '123456' 经过 Bcrypt 算法的结果。

-- 9. 插入一个默认的普通用户 (密码: 123456)
INSERT IGNORE INTO `app_users` (`username`, `password_hash`, `role`)
VALUES
    ('user', '$2a$10$f.2.G.b.H.I.j.k.l.m.n.o.p.q.r.s.t.u.v.w.x.y.z.A.B.C', 'USER');
-- 脚本结束
SELECT '元数据库 (SyncManager) 初始化完成。' AS `Status`;