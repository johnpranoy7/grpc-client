package com.johnp.grpcclient.dto;

public record EnrolledCourseDto(
        long courseId,
        String courseName,
        String term,
        String status,
        float grade
) {
}
