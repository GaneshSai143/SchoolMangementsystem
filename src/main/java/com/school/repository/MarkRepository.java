package com.school.repository;

import com.school.entity.Mark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarkRepository extends JpaRepository<Mark, Long> {
    List<Mark> findByStudentId(Long studentId);
    List<Mark> findByStudentIdAndSubjectAssignmentId(Long studentId, Long subjectAssignmentId);
    List<Mark> findBySubjectAssignmentId(Long subjectAssignmentId);
    List<Mark> findBySubjectAssignment_Classes_Id(Long classId);
    // For dashboard/reports:
    List<Mark> findByStudent_User_Id(Long studentUserId);
}
