package com.johnp.grpcclient.dto;

public record StudentSummaryDto(
        long studentId,
        String firstName,
        String lastName,
        String program,
        double gpa
) {
}
