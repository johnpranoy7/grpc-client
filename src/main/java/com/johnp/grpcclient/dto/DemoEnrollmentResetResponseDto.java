package com.johnp.grpcclient.dto;

public record DemoEnrollmentResetResponseDto(
        int removedCount,
        String message
) {
}
