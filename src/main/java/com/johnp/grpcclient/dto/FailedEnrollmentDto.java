package com.johnp.grpcclient.dto;

public record FailedEnrollmentDto(
        long studentId,
        long courseId,
        String reasonCode,
        String message
) {
}
