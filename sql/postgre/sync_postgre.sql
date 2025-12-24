-- 1. 创建同步事件表
DROP TABLE IF EXISTS sync_event;
CREATE TABLE sync_event (
    id SERIAL PRIMARY KEY,
    table_name VARCHAR(64) NOT NULL,
    op_type VARCHAR(10) NOT NULL CHECK (op_type IN ('INSERT','UPDATE','DELETE')),
    pk_column_name VARCHAR(64) NOT NULL,
    pk_value VARCHAR(255) NOT NULL,
    status INT DEFAULT 0,
    op_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    source_db_type VARCHAR(32) NOT NULL,
    error_msg VARCHAR(1024),
    data_version INT NOT NULL
);
CREATE INDEX idx_sync_status ON sync_event(status);


-- [PG] 1. 通用版本自增函数
CREATE OR REPLACE FUNCTION increment_version() RETURNS TRIGGER AS $$
BEGIN
    IF CURRENT_USER != 'dbsyncer' THEN
        NEW.sync_version := OLD.sync_version + 1;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- [PG] 2. 通用日志记录函数
CREATE OR REPLACE FUNCTION notify_sync_event() RETURNS TRIGGER AS $$
DECLARE
    current_pk_value VARCHAR;
    current_version BIGINT;
    pk_col_name VARCHAR;
BEGIN
    IF CURRENT_USER = 'dbsyncer' THEN RETURN NULL; END IF;
    pk_col_name := TG_ARGV[0];

    IF (TG_OP = 'DELETE') THEN
        EXECUTE 'SELECT $1.' || pk_col_name USING OLD INTO current_pk_value;
        -- DELETE 时记录旧版本号
        EXECUTE 'SELECT $1.sync_version' USING OLD INTO current_version;
    ELSE
        EXECUTE 'SELECT $1.' || pk_col_name USING NEW INTO current_pk_value;
        -- INSERT/UPDATE 时记录新版本号
        EXECUTE 'SELECT $1.sync_version' USING NEW INTO current_version;
    END IF;

    INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version)
    VALUES (TG_TABLE_NAME, TG_OP, pk_col_name, current_pk_value, 0, NOW(), 'POSTGRESQL', current_version);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- users 触发器绑定 (PostgreSQL)
DROP TRIGGER IF EXISTS trg_users_ver ON users;
CREATE TRIGGER trg_users_ver BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION increment_version();

DROP TRIGGER IF EXISTS trg_users_sync ON users;
CREATE TRIGGER trg_users_sync AFTER INSERT OR UPDATE OR DELETE ON users
    FOR EACH ROW EXECUTE FUNCTION notify_sync_event('user_id');

-- students 触发器绑定 (PostgreSQL)
DROP TRIGGER IF EXISTS trg_students_ver ON students;
CREATE TRIGGER trg_students_ver BEFORE UPDATE ON students
    FOR EACH ROW EXECUTE FUNCTION increment_version();

DROP TRIGGER IF EXISTS trg_students_sync ON students;
CREATE TRIGGER trg_students_sync AFTER INSERT OR UPDATE OR DELETE ON students
    FOR EACH ROW EXECUTE FUNCTION notify_sync_event('student_id');

-- courses 触发器绑定 (PostgreSQL)
DROP TRIGGER IF EXISTS trg_courses_ver ON courses;
CREATE TRIGGER trg_courses_ver BEFORE UPDATE ON courses
    FOR EACH ROW EXECUTE FUNCTION increment_version();

DROP TRIGGER IF EXISTS trg_courses_sync ON courses;
CREATE TRIGGER trg_courses_sync AFTER INSERT OR UPDATE OR DELETE ON courses
    FOR EACH ROW EXECUTE FUNCTION notify_sync_event('course_id');

-- enrollments 触发器绑定 (PostgreSQL)
DROP TRIGGER IF EXISTS trg_enrollments_ver ON enrollments;
CREATE TRIGGER trg_enrollments_ver BEFORE UPDATE ON enrollments
    FOR EACH ROW EXECUTE FUNCTION increment_version();

DROP TRIGGER IF EXISTS trg_enrollments_sync ON enrollments;
CREATE TRIGGER trg_enrollments_sync AFTER INSERT OR UPDATE OR DELETE ON enrollments
    FOR EACH ROW EXECUTE FUNCTION notify_sync_event('enrollment_id');

-- books 触发器绑定 (PostgreSQL)
DROP TRIGGER IF EXISTS trg_books_ver ON books;
CREATE TRIGGER trg_books_ver BEFORE UPDATE ON books
    FOR EACH ROW EXECUTE FUNCTION increment_version();

DROP TRIGGER IF EXISTS trg_books_sync ON books;
CREATE TRIGGER trg_books_sync AFTER INSERT OR UPDATE OR DELETE ON books
    FOR EACH ROW EXECUTE FUNCTION notify_sync_event('book_id');

-- borrow_records 触发器绑定 (PostgreSQL)
DROP TRIGGER IF EXISTS trg_borrow_records_ver ON borrow_records;
CREATE TRIGGER trg_borrow_records_ver BEFORE UPDATE ON borrow_records
    FOR EACH ROW EXECUTE FUNCTION increment_version();

DROP TRIGGER IF EXISTS trg_borrow_records_sync ON borrow_records;
CREATE TRIGGER trg_borrow_records_sync AFTER INSERT OR UPDATE OR DELETE ON borrow_records
    FOR EACH ROW EXECUTE FUNCTION notify_sync_event('record_id');

-- shop_items 触发器绑定 (PostgreSQL)
DROP TRIGGER IF EXISTS trg_shop_items_ver ON shop_items;
CREATE TRIGGER trg_shop_items_ver BEFORE UPDATE ON shop_items
    FOR EACH ROW EXECUTE FUNCTION increment_version();

DROP TRIGGER IF EXISTS trg_shop_items_sync ON shop_items;
CREATE TRIGGER trg_shop_items_sync AFTER INSERT OR UPDATE OR DELETE ON shop_items
    FOR EACH ROW EXECUTE FUNCTION notify_sync_event('item_id');

-- orders 触发器绑定 (PostgreSQL)
DROP TRIGGER IF EXISTS trg_orders_ver ON orders;
CREATE TRIGGER trg_orders_ver BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION increment_version();

DROP TRIGGER IF EXISTS trg_orders_sync ON orders;
CREATE TRIGGER trg_orders_sync AFTER INSERT OR UPDATE OR DELETE ON orders
    FOR EACH ROW EXECUTE FUNCTION notify_sync_event('order_id');