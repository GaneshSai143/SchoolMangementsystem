package com.school.repository;

import com.school.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByStudentId(Long studentId);
    List<Feedback> findBySubjectAssignmentId(Long subjectAssignmentId);
    List<Feedback> findBySubjectAssignment_Teacher_Id(Long teacherId); // Feedback submitted by a specific teacher profile

    // For class teacher to find feedback for students in their classes
    @Query("SELECT f FROM Feedback f WHERE f.student.classes.classTeacher.id = :classTeacherUserId")
    List<Feedback> findFeedbackForClassTeacher(@Param("classTeacherUserId") Long classTeacherUserId);

    @Query("SELECT f FROM Feedback f WHERE f.student.classes.classTeacher.id = :classTeacherUserId AND f.isRead = false")
    List<Feedback> findUnreadFeedbackForClassTeacher(@Param("classTeacherUserId") Long classTeacherUserId);

    List<Feedback> findByStudent_Classes_Id(Long classId); // Feedback for students in a specific class
}
