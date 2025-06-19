package com.school.repository;

import com.school.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStudentId(Long studentId);
    List<Task> findByTeacherId(Long teacherId);
    List<Task> findByClassesId(Long classId);
    // Potentially more complex queries, e.g., tasks for a student in a specific class by a specific teacher
}
