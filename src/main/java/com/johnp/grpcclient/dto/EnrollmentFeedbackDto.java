package com.johnp.grpcclient.dto;

public record EnrollmentFeedbackDto(
        long studentId,
        long courseId,
        String status,
        String message,
        double projectedGpa,
        boolean persisted
) {
}
