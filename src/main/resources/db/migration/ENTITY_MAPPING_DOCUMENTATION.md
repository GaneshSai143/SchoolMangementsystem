# Entity to Database Table Mapping Documentation

## Overview
This document provides a comprehensive mapping between JPA Entity classes and their corresponding database tables, including enum mappings and relationships.

## Enum Mappings

### 1. UserRole Enum
**Java Enum:** `com.school.entity.UserRole`
**Database Type:** `user_role_enum`
**Values:**
- `SUPER_ADMIN`
- `ADMIN` (School Principal)
- `TEACHER`
- `STUDENT`
- `PARENT`

### 2. AttendanceStatus Enum
**Java Enum:** `com.school.entity.enums.AttendanceStatus`
**Database Type:** `attendance_status_enum`
**Values:**
- `PRESENT`
- `ABSENT`
- `LATE`
- `EXCUSED_ABSENCE`

### 3. TaskStatus Enum
**Java Enum:** `com.school.entity.enums.TaskStatus`
**Database Type:** `task_status_enum`
**Values:**
- `PENDING`
- `IN_PROGRESS`
- `COMPLETED`
- `CANCELLED`

### 4. TaskPriority Enum
**Java Enum:** `com.school.entity.enums.TaskPriority`
**Database Type:** `task_priority_enum`
**Values:**
- `LOW`
- `MEDIUM`
- `HIGH`

### 5. TaskType Enum
**Java Enum:** `com.school.entity.enums.TaskType`
**Database Type:** `task_type_enum`
**Values:**
- `HOMEWORK`
- `EXAM`
- `QUIZ`
- `PROJECT`
- `OTHER`

## Entity to Table Mappings

### 1. User Entity
**Java Class:** `com.school.entity.User`
**Database Table:** `users`
**Key Fields:**
- `id` (BIGSERIAL PRIMARY KEY)
- `email` (VARCHAR(255) UNIQUE NOT NULL)
- `password` (VARCHAR(255) NOT NULL)
- `first_name` (VARCHAR(255) NOT NULL)
- `last_name` (VARCHAR(255) NOT NULL)
- `role` (user_role_enum NOT NULL)
- `phone_number` (VARCHAR(20))
- `enabled` (BOOLEAN DEFAULT TRUE)
- `school_id` (BIGINT - FK to schools.id)
- `created_at` (TIMESTAMP NOT NULL)
- `updated_at` (TIMESTAMP NOT NULL)
- `auth_provider` (VARCHAR(50))
- `provider_id` (VARCHAR(255))
- `preferred_theme` (VARCHAR(255))

### 2. School Entity
**Java Class:** `com.school.entity.School`
**Database Table:** `schools`
**Key Fields:**
- `id` (BIGSERIAL PRIMARY KEY)
- `name` (VARCHAR(255) NOT NULL)
- `location` (VARCHAR(255) NOT NULL)
- `principal_id` (BIGINT - FK to users.id)
- `created_at` (TIMESTAMP NOT NULL)
- `updated_at` (TIMESTAMP NOT NULL)

### 3. Classes Entity
**Java Class:** `com.school.entity.Classes`
**Database Table:** `classes`
**Key Fields:**
- `id` (BIGSERIAL PRIMARY KEY)
- `name` (VARCHAR(255) NOT NULL)
- `school_id` (BIGINT NOT NULL - FK to schools.id)
- `class_teacher_id` (BIGINT - FK to users.id)
- `created_at` (TIMESTAMP NOT NULL)
- `updated_at` (TIMESTAMP NOT NULL)

### 4. Subject Entity
**Java Class:** `com.school.entity.Subject`
**Database Table:** `subjects`
**Key Fields:**
- `id` (BIGSERIAL PRIMARY KEY)
- `name` (VARCHAR(255) NOT NULL UNIQUE)
- `subject_code` (VARCHAR(255) UNIQUE)
- `created_at` (TIMESTAMP NOT NULL)
- `updated_at` (TIMESTAMP NOT NULL)

### 5. Teacher Entity
**Java Class:** `com.school.entity.Teacher`
**Database Table:** `teachers`
**Key Fields:**
- `id` (BIGSERIAL PRIMARY KEY)
- `user_id` (BIGINT NOT NULL UNIQUE - FK to users.id)
- `created_at` (TIMESTAMP NOT NULL)
- `updated_at` (TIMESTAMP NOT NULL)

**ElementCollection Table:** `teacher_subjects`
- `teacher_id` (BIGINT NOT NULL - FK to teachers.id)
- `subject` (VARCHAR(255) NOT NULL)
- Primary Key: (teacher_id, subject)

### 6. Student Entity
**Java Class:** `com.school.entity.Student`
**Database Table:** `student_profiles`
**Key Fields:**
- `id` (BIGSERIAL PRIMARY KEY)
- `user_id` (BIGINT NOT NULL UNIQUE - FK to users.id)
- `class_id` (BIGINT NOT NULL - FK to classes.id)
- `created_at` (TIMESTAMP NOT NULL)
- `updated_at` (TIMESTAMP NOT NULL)

### 7. SubjectAssignment Entity
**Java Class:** `com.school.entity.SubjectAssignment`
**Database Table:** `subject_assignments`
**Key Fields:**
- `id` (BIGSERIAL PRIMARY KEY)
- `class_id` (BIGINT NOT NULL - FK to classes.id)
- `subject_id` (BIGINT NOT NULL - FK to subjects.id)
- `teacher_id` (BIGINT NOT NULL - FK to teachers.id)
- `academic_year` (VARCHAR(50) NOT NULL)
- `term` (VARCHAR(50) NOT NULL)
- `status` (VARCHAR(50) NOT NULL DEFAULT 'ACTIVE')
- `created_at` (TIMESTAMP NOT NULL)
- `updated_at` (TIMESTAMP NOT NULL)
**Unique Constraint:** (class_id, subject_id, teacher_id, academic_year, term)

### 8. Task Entity
**Java Class:** `com.school.entity.Task`
**Database Table:** `tasks`
**Key Fields:**
- `id` (BIGSERIAL PRIMARY KEY)
- `title` (VARCHAR(255) NOT NULL)
- `description` (TEXT)
- `due_date` (TIMESTAMP NOT NULL)
- `status` (task_status_enum NOT NULL)
- `priority` (task_priority_enum)
- `student_id` (BIGINT - FK to student_profiles.id)
- `teacher_id` (BIGINT - FK to teachers.id)
- `class_id` (BIGINT - FK to classes.id)
- `subject_assignment_id` (BIGINT - FK to subject_assignments.id)
- `task_type` (task_type_enum)
- `created_at` (TIMESTAMP NOT NULL)
- `updated_at` (TIMESTAMP NOT NULL)

### 9. Attendance Entity
**Java Class:** `com.school.entity.Attendance`
**Database Table:** `attendance`
**Key Fields:**
- `id` (BIGSERIAL PRIMARY KEY)
- `student_id` (BIGINT NOT NULL - FK to student_profiles.id)
- `class_id` (BIGINT NOT NULL - FK to classes.id)
- `attendance_date` (DATE NOT NULL)
- `status` (attendance_status_enum NOT NULL)
- `remarks` (TEXT)
- `recorded_by_teacher_id` (BIGINT - FK to teachers.id)
- `created_at` (TIMESTAMP NOT NULL)
- `updated_at` (TIMESTAMP NOT NULL)
**Unique Constraint:** (student_id, attendance_date)

### 10. Mark Entity
**Java Class:** `com.school.entity.Mark`
**Database Table:** `marks`
**Key Fields:**
- `id` (BIGSERIAL PRIMARY KEY)
- `student_id` (BIGINT NOT NULL - FK to student_profiles.id)
- `subject_assignment_id` (BIGINT NOT NULL - FK to subject_assignments.id)
- `assessment_name` (VARCHAR(255) NOT NULL)
- `marks_obtained` (DECIMAL(5,2) NOT NULL)
- `total_marks` (DECIMAL(5,2) NOT NULL)
- `grade` (VARCHAR(10))
- `exam_date` (DATE)
- `comments` (TEXT)
- `recorded_by_teacher_id` (BIGINT - FK to teachers.id)
- `created_at` (TIMESTAMP NOT NULL)
- `updated_at` (TIMESTAMP NOT NULL)

### 11. Feedback Entity
**Java Class:** `com.school.entity.Feedback`
**Database Table:** `feedback`
**Key Fields:**
- `id` (BIGSERIAL PRIMARY KEY)
- `student_id` (BIGINT NOT NULL - FK to student_profiles.id)
- `subject_assignment_id` (BIGINT NOT NULL - FK to subject_assignments.id)
- `feedback_text` (TEXT NOT NULL)
- `submission_date` (TIMESTAMP NOT NULL)
- `is_read` (BOOLEAN NOT NULL DEFAULT FALSE)
- `created_at` (TIMESTAMP NOT NULL)
- `updated_at` (TIMESTAMP NOT NULL)

## Key Relationships

1. **User → School**: Many-to-One (users.school_id → schools.id)
2. **School → User**: One-to-One (schools.principal_id → users.id)
3. **Classes → School**: Many-to-One (classes.school_id → schools.id)
4. **Classes → User**: Many-to-One (classes.class_teacher_id → users.id)
5. **Student → User**: One-to-One (student_profiles.user_id → users.id)
6. **Student → Classes**: Many-to-One (student_profiles.class_id → classes.id)
7. **Teacher → User**: One-to-One (teachers.user_id → users.id)
8. **SubjectAssignment → Classes**: Many-to-One (subject_assignments.class_id → classes.id)
9. **SubjectAssignment → Subject**: Many-to-One (subject_assignments.subject_id → subjects.id)
10. **SubjectAssignment → Teacher**: Many-to-One (subject_assignments.teacher_id → teachers.id)
11. **Task → Student**: Many-to-One (tasks.student_id → student_profiles.id)
12. **Task → Teacher**: Many-to-One (tasks.teacher_id → teachers.id)
13. **Task → Classes**: Many-to-One (tasks.class_id → classes.id)
14. **Task → SubjectAssignment**: Many-to-One (tasks.subject_assignment_id → subject_assignments.id)
15. **Attendance → Student**: Many-to-One (attendance.student_id → student_profiles.id)
16. **Attendance → Classes**: Many-to-One (attendance.class_id → classes.id)
17. **Attendance → Teacher**: Many-to-One (attendance.recorded_by_teacher_id → teachers.id)
18. **Mark → Student**: Many-to-One (marks.student_id → student_profiles.id)
19. **Mark → SubjectAssignment**: Many-to-One (marks.subject_assignment_id → subject_assignments.id)
20. **Mark → Teacher**: Many-to-One (marks.recorded_by_teacher_id → teachers.id)
21. **Feedback → Student**: Many-to-One (feedback.student_id → student_profiles.id)
22. **Feedback → SubjectAssignment**: Many-to-One (feedback.subject_assignment_id → subject_assignments.id)

## Indexes Created

- `idx_users_email` on users(email)
- `idx_users_role` on users(role)
- `idx_users_school_id` on users(school_id)
- `idx_classes_school_id` on classes(school_id)
- `idx_student_profiles_class_id` on student_profiles(class_id)
- `idx_subject_assignments_class_id` on subject_assignments(class_id)
- `idx_subject_assignments_teacher_id` on subject_assignments(teacher_id)
- `idx_tasks_student_id` on tasks(student_id)
- `idx_tasks_teacher_id` on tasks(teacher_id)
- `idx_tasks_class_id` on tasks(class_id)
- `idx_attendance_student_id` on attendance(student_id)
- `idx_attendance_class_id` on attendance(class_id)
- `idx_attendance_date` on attendance(attendance_date)
- `idx_marks_student_id` on marks(student_id)
- `idx_marks_subject_assignment_id` on marks(subject_assignment_id)
- `idx_feedback_student_id` on feedback(student_id)
- `idx_feedback_subject_assignment_id` on feedback(subject_assignment_id)

## Triggers

All tables with `updated_at` columns have triggers that automatically update the timestamp when a row is modified:
- `update_users_updated_at`
- `update_schools_updated_at`
- `update_classes_updated_at`
- `update_subjects_updated_at`
- `update_teachers_updated_at`
- `update_student_profiles_updated_at`
- `update_subject_assignments_updated_at`
- `update_tasks_updated_at`
- `update_attendance_updated_at`
- `update_marks_updated_at`
- `update_feedback_updated_at` 