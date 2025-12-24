DROP DATABASE IF EXISTS VirtualCampus;
CREATE DATABASE VirtualCampus CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE VirtualCampus;

CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role ENUM('student','teacher','admin') DEFAULT 'student',
    email VARCHAR(100),
    phone VARCHAR(20),
    sync_version INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE students (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    student_no VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    gender ENUM('male','female','other') DEFAULT 'other',
    major VARCHAR(100),
    grade INT,
    sync_version INT NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE courses (
    course_id INT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(20) NOT NULL UNIQUE,
    course_name VARCHAR(100) NOT NULL,
    credit DECIMAL(3,1),
    teacher_name VARCHAR(50),
    semester VARCHAR(20),
    sync_version INT NOT NULL DEFAULT 0
);

CREATE TABLE enrollments (
    enrollment_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    enroll_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    grade DECIMAL(4,1),
    sync_version INT NOT NULL DEFAULT 0,
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE
);

CREATE TABLE books (
    book_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(100),
    publisher VARCHAR(100),
    isbn VARCHAR(20) UNIQUE,
    total_copies INT DEFAULT 1,
    available_copies INT DEFAULT 1,
    sync_version INT NOT NULL DEFAULT 0
);

CREATE TABLE borrow_records (
    record_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    book_id INT NOT NULL,
    borrow_date DATE DEFAULT (CURRENT_DATE),
    return_date DATE,
    sync_version INT NOT NULL DEFAULT 0,
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE
);

CREATE TABLE shop_items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    item_name VARCHAR(100) NOT NULL,
    price DECIMAL(8,2) NOT NULL,
    stock INT DEFAULT 0,
    category VARCHAR(50),
    sync_version INT NOT NULL DEFAULT 0
);

CREATE TABLE orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    order_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2),
    status ENUM('pending','paid','shipped','completed','cancelled') DEFAULT 'pending',
    sync_version INT NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

INSERT INTO users (username,password,role,email,phone) VALUES
    ('alice','alice123','student','alice@campus.edu','13800000001'),
    ('bob','bob123','teacher','bob@campus.edu','13800000002'),
    ('admin','admin123','admin','admin@campus.edu','13800000003');

INSERT INTO students (user_id,student_no,name,gender,major,grade) VALUES
    (1,'20250001','Alice Zhang','female','Computer Science',2);

INSERT INTO courses (course_code,course_name,credit,teacher_name,semester) VALUES
    ('CS101','Introduction to Programming',3.0,'Bob','2025-Fall'),
    ('CS201','Database Systems',3.5,'Bob','2025-Fall');

INSERT INTO enrollments (student_id,course_id) VALUES
    (1,1),(1,2);

INSERT INTO books (title,author,publisher,isbn,total_copies,available_copies) VALUES
    ('Introduction to Algorithms','Cormen','MIT Press','9780262033848',5,4),
    ('Computer Networks','Tanenbaum','Pearson','9780132126953',3,3);

INSERT INTO borrow_records (student_id,book_id,borrow_date) VALUES
    (1,1,'2025-11-01');

INSERT INTO shop_items (item_name,price,stock,category) VALUES
    ('Notebook',9.99,100,'Stationery'),
    ('Campus T-shirt',59.90,50,'Clothing'),
    ('Water Bottle',19.80,80,'Daily Goods');

INSERT INTO orders (user_id,order_date,total_amount,status) VALUES
    (1,NOW(),89.70,'paid');
