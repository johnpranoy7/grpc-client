package com.johnp.grpcclient.dto;

public record CourseSummaryDto(
        long courseId,
        String courseName,
        String courseCode,
        String credits
) {
}
