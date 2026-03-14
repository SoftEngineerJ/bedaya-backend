-- MySQL Database Setup for Bedaya Application
-- Run this script in MySQL to create the database and user

-- Create database
CREATE DATABASE IF NOT EXISTS bedaya_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user (optional - you can also use root)
CREATE USER IF NOT EXISTS 'bedaya_user'@'localhost' IDENTIFIED BY 'your_secure_password_here';
GRANT ALL PRIVILEGES ON bedaya_db.* TO 'bedaya_user'@'localhost';
FLUSH PRIVILEGES;

-- Use the database
USE bedaya_db;

-- Tables will be created automatically by Spring Boot JPA
-- with spring.jpa.hibernate.ddl-auto=update

-- For manual table creation (optional):
/*
CREATE TABLE IF NOT EXISTS contact (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    country VARCHAR(100),
    service VARCHAR(100),
    message TEXT NOT NULL,
    status ENUM('NEW', 'IN_PROGRESS', 'COMPLETED') DEFAULT 'NEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);
*/
