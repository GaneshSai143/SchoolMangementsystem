-- Insert super admin user (password: admin123)
INSERT INTO users (username, password, role, created_at, updated_at)
VALUES ('superadmin', '$2a$10$rDkPvvAFV8c4p3KZzXz5UO9ZxZxZxZxZxZxZxZxZxZxZxZxZxZx', 'SUPER_ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert a school
INSERT INTO schools (name, location, created_at, updated_at)
VALUES ('Springfield High School', 'Springfield, IL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert admin user (password: admin123)
INSERT INTO users (username, password, role, school_id, created_at, updated_at)
VALUES ('admin', '$2a$10$rDkPvvAFV8c4p3KZzXz5UO9ZxZxZxZxZxZxZxZxZxZxZxZxZxZx', 'ADMIN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Update school with principal
UPDATE schools SET principal_id = 2 WHERE id = 1;

-- Insert a class
INSERT INTO classes (name, school_id, created_at, updated_at)
VALUES ('Class 10-A', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert a teacher (password: teacher123)
INSERT INTO users (username, password, role, school_id, created_at, updated_at)
VALUES ('teacher1', '$2a$10$rDkPvvAFV8c4p3KZzXz5UO9ZxZxZxZxZxZxZxZxZxZxZxZxZxZx', 'TEACHER', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO teachers (user_id, created_at, updated_at)
VALUES (3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO teacher_subjects (teacher_id, subject)
VALUES (1, 'Mathematics'), (1, 'Physics');

-- Update class with class teacher
UPDATE classes SET class_teacher_id = 3 WHERE id = 1;

-- Insert a student (password: student123)
INSERT INTO users (username, password, role, school_id, created_at, updated_at)
VALUES ('student1', '$2a$10$rDkPvvAFV8c4p3KZzXz5UO9ZxZxZxZxZxZxZxZxZxZxZxZxZxZx', 'STUDENT', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO students (user_id, class_id, created_at, updated_at)
VALUES (4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP); 