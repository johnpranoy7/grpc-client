package com.johnp.grpcclient.dto;

import java.util.List;

public record BatchEnrollResponseDto(
        int successCount,
        int failureCount,
        List<String> failedStudentIds,
        List<FailedEnrollmentDto> failures
) {
}
