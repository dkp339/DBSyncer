-- 若已有同名用户可先清理
BEGIN
    EXECUTE IMMEDIATE 'DROP USER virtualcampus CASCADE';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -01918 THEN
            RAISE;
        END IF;
END;
/

-- 创建数据库用户与表空间
CREATE USER virtualcampus IDENTIFIED BY campus123
    DEFAULT TABLESPACE users
    TEMPORARY TABLESPACE temp
    QUOTA UNLIMITED ON users;

GRANT CONNECT, RESOURCE TO virtualcampus;
ALTER USER virtualcampus ACCOUNT UNLOCK;

-- 切换至虚拟校园用户
ALTER SESSION SET CURRENT_SCHEMA = virtualcampus;

-- 一、创建序列
CREATE SEQUENCE seq_user START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_student START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_course START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_enroll START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_book START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_record START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_item START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_order START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- 二、创建数据表

-- 用户表
CREATE TABLE users (
    user_id NUMBER PRIMARY KEY,
    username VARCHAR2(50) UNIQUE NOT NULL,
    password VARCHAR2(100) NOT NULL,
    role VARCHAR2(20) DEFAULT 'student' CHECK (role IN ('student','teacher','admin')),
    email VARCHAR2(100),
    phone VARCHAR2(20),
    created_at DATE DEFAULT SYSDATE
);

-- 学籍表
CREATE TABLE students (
    student_id NUMBER PRIMARY KEY,
    user_id NUMBER NOT NULL,
    student_no VARCHAR2(20) UNIQUE NOT NULL,
    name VARCHAR2(50) NOT NULL,
    gender VARCHAR2(10) DEFAULT 'other' CHECK (gender IN ('male','female','other')),
    major VARCHAR2(100),
    grade NUMBER,
    CONSTRAINT fk_student_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE
);

-- 课程表
CREATE TABLE courses (
    course_id NUMBER PRIMARY KEY,
    course_code VARCHAR2(20) UNIQUE NOT NULL,
    course_name VARCHAR2(100) NOT NULL,
    credit NUMBER(3,1),
    teacher_name VARCHAR2(50),
    semester VARCHAR2(20)
);

-- 选课表
CREATE TABLE enrollments (
    enrollment_id NUMBER PRIMARY KEY,
    student_id NUMBER NOT NULL,
    course_id NUMBER NOT NULL,
    enroll_date DATE DEFAULT SYSDATE,
    grade NUMBER(4,1),
    CONSTRAINT fk_enroll_student FOREIGN KEY (student_id)
        REFERENCES students(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_enroll_course FOREIGN KEY (course_id)
        REFERENCES courses(course_id) ON DELETE CASCADE
);

-- 图书表
CREATE TABLE books (
    book_id NUMBER PRIMARY KEY,
    title VARCHAR2(200) NOT NULL,
    author VARCHAR2(100),
    publisher VARCHAR2(100),
    isbn VARCHAR2(20) UNIQUE,
    total_copies NUMBER DEFAULT 1,
    available_copies NUMBER DEFAULT 1
);

-- 借阅记录表
CREATE TABLE borrow_records (
    record_id NUMBER PRIMARY KEY,
    student_id NUMBER NOT NULL,
    book_id NUMBER NOT NULL,
    borrow_date DATE DEFAULT SYSDATE,
    return_date DATE,
    CONSTRAINT fk_borrow_student FOREIGN KEY (student_id)
        REFERENCES students(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_borrow_book FOREIGN KEY (book_id)
        REFERENCES books(book_id) ON DELETE CASCADE
);

-- 商店商品表
CREATE TABLE shop_items (
    item_id NUMBER PRIMARY KEY,
    item_name VARCHAR2(100) NOT NULL,
    price NUMBER(8,2) NOT NULL,
    stock NUMBER DEFAULT 0,
    category VARCHAR2(50)
);

-- 订单表
CREATE TABLE orders (
    order_id NUMBER PRIMARY KEY,
    user_id NUMBER NOT NULL,
    order_date DATE DEFAULT SYSDATE,
    total_amount NUMBER(10,2),
    status VARCHAR2(20) DEFAULT 'pending'
        CHECK (status IN ('pending','paid','shipped','completed','cancelled')),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE
);


-- 三、自动编号触发器
CREATE OR REPLACE TRIGGER trg_users
    BEFORE INSERT ON users
    FOR EACH ROW
BEGIN
    IF :NEW.user_id IS NULL THEN
        SELECT seq_user.NEXTVAL INTO :NEW.user_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_students
    BEFORE INSERT ON students
    FOR EACH ROW
BEGIN
    IF :NEW.student_id IS NULL THEN
        SELECT seq_student.NEXTVAL INTO :NEW.student_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_courses
    BEFORE INSERT ON courses
    FOR EACH ROW
BEGIN
    IF :NEW.course_id IS NULL THEN
        SELECT seq_course.NEXTVAL INTO :NEW.course_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_enrollments
    BEFORE INSERT ON enrollments
    FOR EACH ROW
BEGIN
    IF :NEW.enrollment_id IS NULL THEN
        SELECT seq_enroll.NEXTVAL INTO :NEW.enrollment_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_books
    BEFORE INSERT ON books
    FOR EACH ROW
BEGIN
    IF :NEW.book_id IS NULL THEN
        SELECT seq_book.NEXTVAL INTO :NEW.book_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_borrow_records
    BEFORE INSERT ON borrow_records
    FOR EACH ROW
BEGIN
    IF :NEW.record_id IS NULL THEN
        SELECT seq_record.NEXTVAL INTO :NEW.record_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_shop_items
    BEFORE INSERT ON shop_items
    FOR EACH ROW
BEGIN
    IF :NEW.item_id IS NULL THEN
        SELECT seq_item.NEXTVAL INTO :NEW.item_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_orders
    BEFORE INSERT ON orders
    FOR EACH ROW
BEGIN
    IF :NEW.order_id IS NULL THEN
        SELECT seq_order.NEXTVAL INTO :NEW.order_id FROM dual;
    END IF;
END;
/


-- 四、初始化数据

-- 用户数据
INSERT INTO users (username, password, role, email, phone)
VALUES ('alice', 'alice123', 'student', 'alice@campus.edu', '13800000001');
INSERT INTO users (username, password, role, email, phone)
VALUES ('bob', 'bob123', 'teacher', 'bob@campus.edu', '13800000002');
INSERT INTO users (username, password, role, email, phone)
VALUES ('admin', 'admin123', 'admin', 'admin@campus.edu', '13800000003');

-- 学生数据
INSERT INTO students (user_id, student_no, name, gender, major, grade)
VALUES (1, '20250001', 'Alice Zhang', 'female', 'Computer Science', 2);

-- 课程数据
INSERT INTO courses (course_code, course_name, credit, teacher_name, semester)
VALUES ('CS101', 'Introduction to Programming', 3.0, 'Bob', '2025-Fall');
INSERT INTO courses (course_code, course_name, credit, teacher_name, semester)
VALUES ('CS201', 'Database Systems', 3.5, 'Bob', '2025-Fall');

-- 选课数据
INSERT INTO enrollments (student_id, course_id)
VALUES (1, 1);
INSERT INTO enrollments (student_id, course_id)
VALUES (1, 2);

-- 图书数据
INSERT INTO books (title, author, publisher, isbn, total_copies, available_copies)
VALUES ('Introduction to Algorithms', 'Cormen', 'MIT Press', '9780262033848', 5, 4);
INSERT INTO books (title, author, publisher, isbn, total_copies, available_copies)
VALUES ('Computer Networks', 'Tanenbaum', 'Pearson', '9780132126953', 3, 3);

-- 借阅记录
INSERT INTO borrow_records (student_id, book_id, borrow_date)
VALUES (1, 1, TO_DATE('2025-11-01', 'YYYY-MM-DD'));

-- 商店商品
INSERT INTO shop_items (item_name, price, stock, category)
VALUES ('Notebook', 9.99, 100, 'Stationery');
INSERT INTO shop_items (item_name, price, stock, category)
VALUES ('Campus T-shirt', 59.90, 50, 'Clothing');
INSERT INTO shop_items (item_name, price, stock, category)
VALUES ('Water Bottle', 19.80, 80, 'Daily Goods');

-- 订单数据
INSERT INTO orders (user_id, order_date, total_amount, status)
VALUES (1, SYSDATE, 89.70, 'paid');

COMMIT;


-- 五、检查表与数据
-- SELECT table_name FROM user_tables;
-- SELECT * FROM users;
-- SELECT * FROM students;
-- SELECT * FROM courses;
-- SELECT * FROM enrollments;
-- SELECT * FROM books;
-- SELECT * FROM borrow_records;
-- SELECT * FROM shop_items;
-- SELECT * FROM orders;

