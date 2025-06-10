-- Drop existing tables if they exist
DROP TABLE IF EXISTS tasks CASCADE;
DROP TABLE IF EXISTS students CASCADE;
DROP TABLE IF EXISTS teachers CASCADE;
DROP TABLE IF EXISTS classes CASCADE;
DROP TABLE IF EXISTS schools CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS teacher_subjects CASCADE;

-- Create users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    school_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create schools table
CREATE TABLE schools (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    principal_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (principal_id) REFERENCES users(id)
);

-- Add foreign key to users table
ALTER TABLE users
ADD CONSTRAINT fk_users_school
FOREIGN KEY (school_id) REFERENCES schools(id);

-- Create classes table
CREATE TABLE classes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    school_id BIGINT NOT NULL,
    class_teacher_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (school_id) REFERENCES schools(id),
    FOREIGN KEY (class_teacher_id) REFERENCES users(id)
);

-- Create teachers table
CREATE TABLE teachers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create teacher_subjects table
CREATE TABLE teacher_subjects (
    teacher_id BIGINT NOT NULL,
    subject VARCHAR(255) NOT NULL,
    PRIMARY KEY (teacher_id, subject),
    FOREIGN KEY (teacher_id) REFERENCES teachers(id)
);

-- Create students table
CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    class_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (class_id) REFERENCES classes(id)
);

-- Create tasks table
CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    assigned_by BIGINT NOT NULL,
    assigned_to BIGINT NOT NULL,
    due_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (assigned_by) REFERENCES teachers(id),
    FOREIGN KEY (assigned_to) REFERENCES students(id)
); 