/*-- Insert super admin user (password: admin123)
INSERT INTO users (email, password, first_name, last_name, role, enabled, created_at, updated_at)
VALUES (
    'superadmin@school.com',
    '$2a$10$rDkPvvAFV8c4p3KZzXz5UO9ZxZxZxZxZxZxZxZxZxZxZxZxZxZx',
    'Super',
    'Admin',
    'SUPER_ADMIN',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Insert admin user (password: admin123)
INSERT INTO users (email, password, first_name, last_name, role, enabled, created_at, updated_at)
VALUES (
    'admin@springfield.edu',
    '$2a$10$rDkPvvAFV8c4p3KZzXz5UO9ZxZxZxZxZxZxZxZxZxZxZxZxZxZx',
    'John',
    'Principal',
    'ADMIN',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Insert a teacher (password: teacher123)
INSERT INTO users (email, password, first_name, last_name, role, enabled, created_at, updated_at)
VALUES (
    'teacher@springfield.edu',
    '$2a$10$rDkPvvAFV8c4p3KZzXz5UO9ZxZxZxZxZxZxZxZxZxZxZxZxZxZx',
    'Jane',
    'Teacher',
    'TEACHER',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Insert a student (password: student123)
INSERT INTO users (email, password, first_name, last_name, role, enabled, created_at, updated_at)
VALUES (
    'student@springfield.edu',
    '$2a$10$rDkPvvAFV8c4p3KZzXz5UO9ZxZxZxZxZxZxZxZxZxZxZxZxZx',
    'Bob',
    'Student',
    'STUDENT',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Insert default school
INSERT INTO schools (name, address, phone, email, website, principal_id, created_at, updated_at)
VALUES (
    'Springfield High School',
    '123 Education St, Springfield',
    '555-0123',
    'contact@springfield.edu',
    'www.springfield.edu',
    1, -- principal_id references the admin user
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Update users with school_id
UPDATE users SET school_id = 1 WHERE id IN (1, 2, 3);

-- Insert a class
INSERT INTO classes (name, grade_level, school_id, teacher_id, created_at, updated_at)
VALUES (
    'Class 10-A',
    '10',
    1,
    2, -- teacher_id references the teacher user
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Update student profile
INSERT INTO student_profiles (user_id, class_id, roll_number, date_of_birth, gender, address, parent_name, parent_phone, parent_email, created_at, updated_at)
VALUES (
    3, -- student user id
    1, -- class id
    'S001',
    '2005-01-01',
    'MALE',
    '456 Student Ave, Springfield',
    'Parent Name',
    '555-0124',
    'parent@email.com',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);*/