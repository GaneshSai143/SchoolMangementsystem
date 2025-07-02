package com.school.service.impl;

import com.school.dto.*; // All DTOs
import com.school.entity.*;
import com.school.exception.ResourceNotFoundException;
import com.school.exception.UnauthorizedActionException;
import com.school.repository.*;
import com.school.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final StudentRepository studentRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final TeacherRepository teacherRepository; // To get Teacher Profile
    private final ModelMapper modelMapper;
    private final UserServiceImpl userServiceImpl; // For nested UserDTOs
    private final SubjectAssignmentServiceImpl subjectAssignmentServiceImpl; // For nested SubjectAssignmentResponseDTO

    @Override
    @Transactional
    public FeedbackResponseDTO submitFeedback(CreateFeedbackRequestDTO requestDTO, User currentUser) {
        Teacher submittingTeacherProfile = teacherRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new UnauthorizedActionException("User is not a registered teacher profile."));

        SubjectAssignment assignment = subjectAssignmentRepository.findById(requestDTO.getSubjectAssignmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject Assignment not found: " + requestDTO.getSubjectAssignmentId()));

        // Authorization: Only the teacher assigned to the SubjectAssignment can submit feedback for it.
        if (!assignment.getTeacher().getId().equals(submittingTeacherProfile.getId())) {
            throw new UnauthorizedActionException("Teacher is not authorized to submit feedback for this subject assignment.");
        }

        Student student = studentRepository.findById(requestDTO.getStudentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + requestDTO.getStudentId()));

        // Authorization: Ensure student belongs to the class of the subject assignment
        if (!assignment.getClasses().getId().equals(student.getClasses().getId())) {
            throw new IllegalArgumentException("Student does not belong to the class of this subject assignment.");
        }

        Feedback feedback = Feedback.builder()
            .student(student)
            .subjectAssignment(assignment)
            .feedbackText(requestDTO.getFeedbackText())
            .isRead(false)
            // submissionDate and createdAt will be set by @CreatedDate
            .build();
        Feedback savedFeedback = feedbackRepository.save(feedback);
        return convertToDTO(savedFeedback);
    }

    @Override
    public FeedbackResponseDTO getFeedbackById(Long id, User currentUser) {
        Feedback feedback = feedbackRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Feedback not found: " + id));
        // Authorization: Student (parent), involved teachers, admin can view.
        authorizeView(feedback, currentUser);
        return convertToDTO(feedback);
    }

    @Override
    public List<FeedbackResponseDTO> getFeedbackByStudentId(Long studentId, User currentUser) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        // Authorization: Student (parent), their class teacher, subject teachers who taught them, admin.
        // Simplified: if current user is student, check ID. If teacher, check if class teacher or involved subject teacher.
        if (currentUser.getRole() == UserRole.STUDENT && !currentUser.getId().equals(student.getUser().getId())) {
             throw new UnauthorizedActionException("Students can only view their own feedback.");
        }
        // More granular checks for teachers needed here based on their relation to the student.
        // For now, admins and the student themselves can view. Teachers' access via getFeedbackForClassTeacher.

        List<Feedback> feedbacks = feedbackRepository.findByStudentId(studentId);
        return feedbacks.stream().map(this::convertToDTO).collect(Collectors.toList());
    }


    @Override
    public List<FeedbackResponseDTO> getFeedbackForClassTeacher(User classTeacherUser) {
        if (classTeacherUser.getRole() != UserRole.TEACHER) { // Or ADMIN if they act as class teacher
            throw new UnauthorizedActionException("User is not authorized to view class teacher feedback.");
        }
        // The query in repository uses user_id of the class teacher from classes.classTeacher field
        List<Feedback> feedbacks = feedbackRepository.findFeedbackForClassTeacher(classTeacherUser.getId());
        return feedbacks.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

     @Override
    public List<FeedbackResponseDTO> getUnreadFeedbackForClassTeacher(User classTeacherUser) {
        if (classTeacherUser.getRole() != UserRole.TEACHER) {
             throw new UnauthorizedActionException("User is not authorized to view class teacher feedback.");
        }
        List<Feedback> feedbacks = feedbackRepository.findUnreadFeedbackForClassTeacher(classTeacherUser.getId());
        return feedbacks.stream().map(this::convertToDTO).collect(Collectors.toList());
    }


    @Override
    @Transactional
    public FeedbackResponseDTO markFeedbackAsRead(Long feedbackId, User classTeacherUser) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new ResourceNotFoundException("Feedback not found: " + feedbackId));

        // Authorization: Only the class teacher of the student in the feedback can mark it as read.
        if (classTeacherUser.getRole() != UserRole.TEACHER ||
            feedback.getStudent().getClasses().getClassTeacher() == null || // Check if class teacher is assigned
            !feedback.getStudent().getClasses().getClassTeacher().getId().equals(classTeacherUser.getId())) {
            throw new UnauthorizedActionException("User is not authorized to mark this feedback as read.");
        }

        feedback.setRead(true);
        Feedback updatedFeedback = feedbackRepository.save(feedback);
        return convertToDTO(updatedFeedback);
    }

    private void authorizeView(Feedback feedback, User currentUser) {
        // SuperAdmin/Admin can view all
        if (currentUser.getRole() == UserRole.SUPER_ADMIN || currentUser.getRole() == UserRole.ADMIN) return;

        // Student can view their own feedback
        if (currentUser.getRole() == UserRole.STUDENT && feedback.getStudent().getUser().getId().equals(currentUser.getId())) return;

        // Teacher specific logic
        if (currentUser.getRole() == UserRole.TEACHER) {
            Teacher currentTeacherProfile = teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedActionException("Teacher profile not found for current user."));

            // Is it the class teacher of the student?
            User classTeacherOfStudent = feedback.getStudent().getClasses().getClassTeacher();
            if (classTeacherOfStudent != null && classTeacherOfStudent.getId().equals(currentUser.getId())) return;

            // Is it the teacher who submitted this specific feedback (via subject assignment)?
            if (feedback.getSubjectAssignment().getTeacher().getId().equals(currentTeacherProfile.getId())) return;
        }
        throw new UnauthorizedActionException("User is not authorized to view this feedback.");
    }

    private FeedbackResponseDTO convertToDTO(Feedback feedback) {
        FeedbackResponseDTO dto = modelMapper.map(feedback, FeedbackResponseDTO.class);
        if (feedback.getStudent() != null) {
            StudentDTO studentDTO = modelMapper.map(feedback.getStudent(), StudentDTO.class);
            if(feedback.getStudent().getUser() != null) {
                studentDTO.setUser(userServiceImpl.convertToDTO(feedback.getStudent().getUser()));
            }
            if(feedback.getStudent().getClasses() != null) {
                studentDTO.setClassName(feedback.getStudent().getClasses().getName());
                studentDTO.setClassId(feedback.getStudent().getClasses().getId());
            }
            dto.setStudent(studentDTO);
        }
        if (feedback.getSubjectAssignment() != null) {
            // Use the existing service method if it provides full conversion
            dto.setSubjectAssignment(subjectAssignmentServiceImpl.convertToResponseDTO(feedback.getSubjectAssignment()));
        }
        return dto;
    }
}
