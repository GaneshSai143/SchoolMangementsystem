package com.school.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDashboardDTO {
    private int pendingTasksCount;
    private List<TaskSummaryDTO> recentPendingTasks; // e.g., next 5 due
    private List<PerformanceItemDTO> performanceSummary; // e.g., by subject for recent period
    // Could also include recent attendance summary if desired
}
