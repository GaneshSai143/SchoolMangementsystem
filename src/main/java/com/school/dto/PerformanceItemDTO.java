package com.school.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceItemDTO {
    private String subjectName;
    private BigDecimal averagePercentage; // Calculated as (sum(marksObtained)/sum(totalMarks)) * 100
    private String periodDescription; // e.g., "Last 7 days", "Overall"
}
