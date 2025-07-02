package com.school.service;

import com.school.dto.CreateFeedbackRequestDTO;
import com.school.dto.FeedbackResponseDTO;
import com.school.entity.User;
import java.util.List;

public interface FeedbackService {
    FeedbackResponseDTO submitFeedback(CreateFeedbackRequestDTO requestDTO, User currentUser);
    FeedbackResponseDTO getFeedbackById(Long id, User currentUser);
    List<FeedbackResponseDTO> getFeedbackByStudentId(Long studentId, User currentUser);
    List<FeedbackResponseDTO> getFeedbackForClassTeacher(User classTeacherUser); // Gets all for their students
    List<FeedbackResponseDTO> getUnreadFeedbackForClassTeacher(User classTeacherUser);
    FeedbackResponseDTO markFeedbackAsRead(Long feedbackId, User classTeacherUser);
}
