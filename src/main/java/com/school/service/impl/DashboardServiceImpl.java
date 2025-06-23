package com.school.service.impl;

import com.school.dto.*; // All DTOs
import com.school.entity.Mark;
import com.school.entity.Student;
import com.school.entity.Task;
import com.school.entity.User;
import com.school.entity.enums.TaskStatus; // Entity Enum for comparison
import com.school.exception.ResourceNotFoundException;
import com.school.repository.MarkRepository;
import com.school.repository.StudentRepository;
import com.school.repository.TaskRepository;
import com.school.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final StudentRepository studentRepository;
    private final TaskRepository taskRepository;
    private final MarkRepository markRepository;
    private final ModelMapper modelMapper;

    @Override
    public StudentDashboardDTO getStudentDashboard(User currentUser) {
        Student studentProfile = studentRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for current user."));

        // 1. Pending Tasks
        List<Task> allStudentTasks = taskRepository.findByStudentId(studentProfile.getId());
        List<TaskSummaryDTO> pendingTasksSummary = allStudentTasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.CANCELLED)
                .sorted(Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(5) // Show, for example, the next 5 pending tasks
                .map(this::convertToTaskSummaryDTO)
                .collect(Collectors.toList());

        int pendingTasksCount = (int) allStudentTasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.CANCELLED)
                .count();

        // 2. Performance Summary (e.g., overall average per subject for last 30 days)
        // For simplicity, using all marks of the student.
        List<Mark> allStudentMarks = markRepository.findByStudentId(studentProfile.getId());

        Map<String, List<Mark>> marksBySubject = allStudentMarks.stream()
            .filter(mark -> mark.getSubjectAssignment() != null && mark.getSubjectAssignment().getSubject() != null)
            .collect(Collectors.groupingBy(mark -> mark.getSubjectAssignment().getSubject().getName()));

        List<PerformanceItemDTO> performanceItems = marksBySubject.entrySet().stream()
            .map(entry -> {
                String subjectName = entry.getKey();
                List<Mark> subjectMarks = entry.getValue();
                BigDecimal totalObtained = subjectMarks.stream().map(Mark::getMarksObtained).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalPossible = subjectMarks.stream().map(Mark::getTotalMarks).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal averagePercentage = BigDecimal.ZERO;
                if (totalPossible.compareTo(BigDecimal.ZERO) > 0) {
                    averagePercentage = totalObtained.multiply(new BigDecimal("100.00"))
                                                     .divide(totalPossible, 2, RoundingMode.HALF_UP);
                }
                return PerformanceItemDTO.builder()
                        .subjectName(subjectName)
                        .averagePercentage(averagePercentage)
                        .periodDescription("Overall")
                        .build();
            })
            .sorted(Comparator.comparing(PerformanceItemDTO::getSubjectName))
            .collect(Collectors.toList());


        return StudentDashboardDTO.builder()
                .pendingTasksCount(pendingTasksCount)
                .recentPendingTasks(pendingTasksSummary)
                .performanceSummary(performanceItems)
                .build();
    }

    private TaskSummaryDTO convertToTaskSummaryDTO(Task task) {
        TaskSummaryDTO dto = new TaskSummaryDTO();

        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDueDate(task.getDueDate());

        // Logic for subjectName
        if (task.getSubjectAssignment() != null &&
            task.getSubjectAssignment().getSubject() != null &&
            task.getSubjectAssignment().getSubject().getName() != null) {
            dto.setSubjectName(task.getSubjectAssignment().getSubject().getName());
        } else if (task.getClasses() != null) { // Fallback if no specific subject assignment link
            dto.setSubjectName("Class Task (" + task.getClasses().getName() + ")");
        } else {
            dto.setSubjectName("General Task"); // Further fallback
        }

        // Map status enum
        if (task.getStatus() != null) {
            dto.setStatus(modelMapper.map(task.getStatus(), TaskStatusDTO.class));
        } else {
            dto.setStatus(null);
        }

        return dto;
    }
}
