package com.johnp.grpcclient.dto;

public record EnrollmentIntentDto(
        long studentId,
        long courseId,
        String term,
        String status,
        double grade
) {
}
