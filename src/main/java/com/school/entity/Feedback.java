package com.school.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "feedback")
@EntityListeners(AuditingEntityListener.class)
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student; // Student profile

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_assignment_id", nullable = false)
    private SubjectAssignment subjectAssignment; // Context of class, subject, teacher

    @Column(name = "feedback_text", columnDefinition = "TEXT", nullable = false)
    private String feedbackText;

    @Column(name = "submission_date", nullable = false, updatable = false)
    @CreatedDate // Use @CreatedDate for submission_date as it's set on creation
    private LocalDateTime submissionDate;

    @Column(name = "is_read", nullable = false)
    @ColumnDefault("false")
    private boolean isRead = false;

    // Timestamps for the record itself, submissionDate is specific to feedback event
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
