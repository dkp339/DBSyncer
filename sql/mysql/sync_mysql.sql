USE VirtualCampus;

-- 创建事件表
DROP TABLE IF EXISTS sync_event;
CREATE TABLE sync_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(64) NOT NULL,
    op_type VARCHAR(20) NOT NULL COMMENT 'INSERT, UPDATE, DELETE',
    pk_column_name VARCHAR(64) NOT NULL,
    pk_value VARCHAR(255) NOT NULL,
    status TINYINT DEFAULT 0 COMMENT '0:未同步 1:已同步 2:失败',
    op_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    source_db_type VARCHAR(32) NOT NULL COMMENT '源数据库类型',
    error_msg TEXT COMMENT '错误日志',
    data_version INT NOT NULL COMMENT '乐观锁版本号',
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建触发器
DELIMITER $$
-- [MySQL] users 乐观锁触发器
DROP TRIGGER IF EXISTS trg_users_before_update $$
CREATE TRIGGER trg_users_before_update BEFORE UPDATE ON users FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        SET NEW.sync_version = OLD.sync_version + 1;
    END IF;
END $$

-- [MySQL] users 同步日志触发器
DROP TRIGGER IF EXISTS trg_users_insert $$
CREATE TRIGGER trg_users_insert AFTER INSERT ON users FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('users', 'INSERT', 'user_id', CAST(NEW.user_id AS CHAR), 0, NOW(), 'MYSQL', NEW.sync_version);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_users_update $$
CREATE TRIGGER trg_users_update AFTER UPDATE ON users FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('users', 'UPDATE', 'user_id', CAST(NEW.user_id AS CHAR), 0, NOW(), 'MYSQL', NEW.sync_version);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_users_delete $$
CREATE TRIGGER trg_users_delete AFTER DELETE ON users FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('users', 'DELETE', 'user_id', CAST(OLD.user_id AS CHAR), 0, NOW(), 'MYSQL', OLD.sync_version);
    END IF;
END $$
DELIMITER ;

DELIMITER $$
-- [MySQL] students 乐观锁触发器
DROP TRIGGER IF EXISTS trg_students_before_update $$
CREATE TRIGGER trg_students_before_update BEFORE UPDATE ON students FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        SET NEW.sync_version = OLD.sync_version + 1;
    END IF;
END $$

-- [MySQL] students 同步日志触发器
DROP TRIGGER IF EXISTS trg_students_insert $$
CREATE TRIGGER trg_students_insert AFTER INSERT ON students FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('students', 'INSERT', 'student_id', CAST(NEW.student_id AS CHAR), 0, NOW(), 'MYSQL', NEW.sync_version);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_students_update $$
CREATE TRIGGER trg_students_update AFTER UPDATE ON students FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('students', 'UPDATE', 'student_id', CAST(NEW.student_id AS CHAR), 0, NOW(), 'MYSQL', NEW.sync_version);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_students_delete $$
CREATE TRIGGER trg_students_delete AFTER DELETE ON students FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('students', 'DELETE', 'student_id', CAST(OLD.student_id AS CHAR), 0, NOW(), 'MYSQL', OLD.sync_version);
    END IF;
END $$
DELIMITER ;

DELIMITER $$
-- [MySQL] courses 乐观锁触发器
DROP TRIGGER IF EXISTS trg_courses_before_update $$
CREATE TRIGGER trg_courses_before_update BEFORE UPDATE ON courses FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        SET NEW.sync_version = OLD.sync_version + 1;
    END IF;
END $$

-- [MySQL] courses 同步日志触发器
DROP TRIGGER IF EXISTS trg_courses_insert $$
CREATE TRIGGER trg_courses_insert AFTER INSERT ON courses FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('courses', 'INSERT', 'course_id', CAST(NEW.course_id AS CHAR), 0, NOW(), 'MYSQL', NEW.sync_version);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_courses_update $$
CREATE TRIGGER trg_courses_update AFTER UPDATE ON courses FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('courses', 'UPDATE', 'course_id', CAST(NEW.course_id AS CHAR), 0, NOW(), 'MYSQL', NEW.sync_version);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_courses_delete $$
CREATE TRIGGER trg_courses_delete AFTER DELETE ON courses FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('courses', 'DELETE', 'course_id', CAST(OLD.course_id AS CHAR), 0, NOW(), 'MYSQL', OLD.sync_version);
    END IF;
END $$
DELIMITER ;

DELIMITER $$
-- [MySQL] enrollments 乐观锁触发器
DROP TRIGGER IF EXISTS trg_enrollments_before_update $$
CREATE TRIGGER trg_enrollments_before_update BEFORE UPDATE ON enrollments FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        SET NEW.sync_version = OLD.sync_version + 1;
    END IF;
END $$

-- [MySQL] enrollments 同步日志触发器
DROP TRIGGER IF EXISTS trg_enrollments_insert $$
CREATE TRIGGER trg_enrollments_insert AFTER INSERT ON enrollments FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('enrollments', 'INSERT', 'enrollment_id', CAST(NEW.enrollment_id AS CHAR), 0, NOW(), 'MYSQL', NEW.sync_version);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_enrollments_update $$
CREATE TRIGGER trg_enrollments_update AFTER UPDATE ON enrollments FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('enrollments', 'UPDATE', 'enrollment_id', CAST(NEW.enrollment_id AS CHAR), 0, NOW(), 'MYSQL', NEW.sync_version);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_enrollments_delete $$
CREATE TRIGGER trg_enrollments_delete AFTER DELETE ON enrollments FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('enrollments', 'DELETE', 'enrollment_id', CAST(OLD.enrollment_id AS CHAR), 0, NOW(), 'MYSQL', OLD.sync_version);
    END IF;
END $$
DELIMITER ;

DELIMITER $$
-- [MySQL] books 乐观锁触发器
DROP TRIGGER IF EXISTS trg_books_before_update $$
CREATE TRIGGER trg_books_before_update BEFORE UPDATE ON books FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        SET NEW.sync_version = OLD.sync_version + 1;
    END IF;
END $$

-- [MySQL] books 同步日志触发器
DROP TRIGGER IF EXISTS trg_books_insert $$
CREATE TRIGGER trg_books_insert AFTER INSERT ON books FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('books', 'INSERT', 'book_id', CAST(NEW.book_id AS CHAR), 0, NOW(), 'MYSQL', NEW.sync_version);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_books_update $$
CREATE TRIGGER trg_books_update AFTER UPDATE ON books FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('books', 'UPDATE', 'book_id', CAST(NEW.book_id AS CHAR), 0, NOW(), 'MYSQL', NEW.sync_version);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_books_delete $$
CREATE TRIGGER trg_books_delete AFTER DELETE ON books FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('books', 'DELETE', 'book_id', CAST(OLD.book_id AS CHAR), 0, NOW(), 'MYSQL', OLD.sync_version);
    END IF;
END $$
DELIMITER ;

DELIMITER $$
-- [MySQL] borrow_records 乐观锁触发器
DROP TRIGGER IF EXISTS trg_borrow_records_before_update $$
CREATE TRIGGER trg_borrow_records_before_update BEFORE UPDATE ON borrow_records FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        SET NEW.sync_version = OLD.sync_version + 1;
    END IF;
END $$

-- [MySQL] borrow_records 同步日志触发器
DROP TRIGGER IF EXISTS trg_borrow_records_insert $$
CREATE TRIGGER trg_borrow_records_insert AFTER INSERT ON borrow_records FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('borrow_records', 'INSERT', 'record_id', CAST(NEW.record_id AS CHAR), 0, NOW(), 'MYSQL', NEW.sync_version);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_borrow_records_update $$
CREATE TRIGGER trg_borrow_records_update AFTER UPDATE ON borrow_records FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('borrow_records', 'UPDATE', 'record_id', CAST(NEW.record_id AS CHAR), 0, NOW(), 'MYSQL', NEW.sync_version);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_borrow_records_delete $$
CREATE TRIGGER trg_borrow_records_delete AFTER DELETE ON borrow_records FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('borrow_records', 'DELETE', 'record_id', CAST(OLD.record_id AS CHAR), 0, NOW(), 'MYSQL', OLD.sync_version);
    END IF;
END $$
DELIMITER ;

DELIMITER $$
-- [MySQL] shop_items 乐观锁触发器
DROP TRIGGER IF EXISTS trg_shop_items_before_update $$
CREATE TRIGGER trg_shop_items_before_update BEFORE UPDATE ON shop_items FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        SET NEW.sync_version = OLD.sync_version + 1;
    END IF;
END $$

-- [MySQL] shop_items 同步日志触发器
DROP TRIGGER IF EXISTS trg_shop_items_insert $$
CREATE TRIGGER trg_shop_items_insert AFTER INSERT ON shop_items FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('shop_items', 'INSERT', 'item_id', CAST(NEW.item_id AS CHAR), 0, NOW(), 'MYSQL', NEW.sync_version);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_shop_items_update $$
CREATE TRIGGER trg_shop_items_update AFTER UPDATE ON shop_items FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('shop_items', 'UPDATE', 'item_id', CAST(NEW.item_id AS CHAR), 0, NOW(), 'MYSQL', NEW.sync_version);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_shop_items_delete $$
CREATE TRIGGER trg_shop_items_delete AFTER DELETE ON shop_items FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('shop_items', 'DELETE', 'item_id', CAST(OLD.item_id AS CHAR), 0, NOW(), 'MYSQL', OLD.sync_version);
    END IF;
END $$
DELIMITER ;

DELIMITER $$
-- [MySQL] orders 乐观锁触发器
DROP TRIGGER IF EXISTS trg_orders_before_update $$
CREATE TRIGGER trg_orders_before_update BEFORE UPDATE ON orders FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        SET NEW.sync_version = OLD.sync_version + 1;
    END IF;
END $$

-- [MySQL] orders 同步日志触发器
DROP TRIGGER IF EXISTS trg_orders_insert $$
CREATE TRIGGER trg_orders_insert AFTER INSERT ON orders FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('orders', 'INSERT', 'order_id', CAST(NEW.order_id AS CHAR), 0, NOW(), 'MYSQL', NEW.sync_version);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_orders_update $$
CREATE TRIGGER trg_orders_update AFTER UPDATE ON orders FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('orders', 'UPDATE', 'order_id', CAST(NEW.order_id AS CHAR), 0, NOW(), 'MYSQL', NEW.sync_version);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_orders_delete $$
CREATE TRIGGER trg_orders_delete AFTER DELETE ON orders FOR EACH ROW BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) != 'dbsyncer' THEN
        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
        VALUES ('orders', 'DELETE', 'order_id', CAST(OLD.order_id AS CHAR), 0, NOW(), 'MYSQL', OLD.sync_version);
    END IF;
END $$
DELIMITER ;