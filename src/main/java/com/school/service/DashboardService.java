package com.school.service;

import com.school.dto.StudentDashboardDTO;
import com.school.entity.User;

public interface DashboardService {
    StudentDashboardDTO getStudentDashboard(User currentUser);
}
