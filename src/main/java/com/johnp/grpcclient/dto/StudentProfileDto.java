package com.johnp.grpcclient.dto;

import java.util.List;

public record StudentProfileDto(
        long studentId,
        String firstName,
        String lastName,
        String email,
        String program,
        int currentSemester,
        double gpa,
        String enrollmentDate,
        List<EnrolledCourseDto> enrolledCourses
) {
}
