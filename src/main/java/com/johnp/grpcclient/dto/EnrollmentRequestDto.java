package com.johnp.grpcclient.dto;

public record EnrollmentRequestDto(
        long studentId,
        long courseId,
        String term,
        String status,
        double grade
) {
}
