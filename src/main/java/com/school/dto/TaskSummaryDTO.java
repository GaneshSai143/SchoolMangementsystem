package com.school.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSummaryDTO {
    private Long id;
    private String title;
    private LocalDateTime dueDate;
    private String subjectName; // From SubjectAssignment -> Subject
    private TaskStatusDTO status;
}
