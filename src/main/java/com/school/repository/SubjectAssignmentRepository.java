package com.school.repository;

import com.school.entity.SubjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectAssignmentRepository extends JpaRepository<SubjectAssignment, Long> {
    List<SubjectAssignment> findByClassesId(Long classId);
    List<SubjectAssignment> findByTeacherId(Long teacherId);
    List<SubjectAssignment> findBySubjectId(Long subjectId);
    List<SubjectAssignment> findByClassesIdAndStatus(Long classId, String status);
    List<SubjectAssignment> findByTeacherIdAndStatus(Long teacherId, String status);
    boolean existsByClassesIdAndTeacherIdAndStatus(Long classId, Long teacherId, String status);
    // Add more specific finders as needed, e.g., by teacher, class, and academic term
}
