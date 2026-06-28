package com.johnp.grpcclient.mapper;

import com.google.protobuf.Timestamp;
import com.johnp.grpc.*;
import com.johnp.grpcclient.dto.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ProtoMapper {

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_INSTANT;

    private ProtoMapper() {
    }

    public static StudentProfileDto toDto(StudentProfileResponse response) {
        List<EnrolledCourseDto> courses = response.getEnrolledCoursesList().stream()
                .map(ProtoMapper::toDto)
                .toList();

        return new StudentProfileDto(
                response.getStudentId(),
                response.getFirstName(),
                response.getLastName(),
                response.getEmail(),
                response.getProgram(),
                response.getCurrentSemester(),
                response.getGpa(),
                formatTimestamp(response.getEnrollmentDate()),
                courses);
    }

    public static EnrolledCourseDto toDto(EnrolledCourse course) {
        return new EnrolledCourseDto(
                course.getCourseId(),
                course.getCourseName(),
                course.getTerm(),
                course.getStatus(),
                course.getGrade());
    }

    public static CourseSummaryDto toDto(CourseSummary course) {
        return new CourseSummaryDto(
                course.getCourseId(),
                course.getCourseName(),
                course.getCourseCode(),
                course.getCredits());
    }

    public static StudentSummaryDto toDto(StudentSummary student) {
        return new StudentSummaryDto(
                student.getStudentId(),
                student.getFirstName(),
                student.getLastName(),
                student.getProgram(),
                student.getGpa());
    }

    public static BatchEnrollResponseDto toDto(BatchEnrollStudentsResponse response) {
        List<FailedEnrollmentDto> failures = response.getFailuresList().stream()
                .map(ProtoMapper::toDto)
                .toList();

        return new BatchEnrollResponseDto(
                response.getSuccessCount(),
                response.getFailureCount(),
                response.getFailedStudentIdsList(),
                failures);
    }

    public static FailedEnrollmentDto toDto(FailedEnrollment failure) {
        return new FailedEnrollmentDto(
                failure.getStudentId(),
                failure.getCourseId(),
                failure.getReasonCode(),
                failure.getMessage());
    }

    public static EnrollmentFeedbackDto toDto(EnrollmentFeedback feedback) {
        return new EnrollmentFeedbackDto(
                feedback.getStudentId(),
                feedback.getCourseId(),
                feedback.getStatus(),
                feedback.getMessage(),
                feedback.getProjectedGpa(),
                feedback.getPersisted());
    }

    public static StudentEnrollmentRequest toProto(EnrollmentRequestDto dto) {
        return StudentEnrollmentRequest.newBuilder()
                .setStudentId(dto.studentId())
                .setCourseId(dto.courseId())
                .setTerm(dto.term())
                .setStatus(dto.status())
                .setGrade(dto.grade())
                .build();
    }

    public static EnrollmentIntent toProto(EnrollmentIntentDto dto) {
        return EnrollmentIntent.newBuilder()
                .setStudentId(dto.studentId())
                .setCourseId(dto.courseId())
                .setTerm(dto.term())
                .setStatus(dto.status())
                .setGrade(dto.grade())
                .build();
    }

    public static DemoEnrollmentResetResponseDto toDto(DemoEnrollmentResetResponse response) {
        return new DemoEnrollmentResetResponseDto(
                response.getRemovedCount(),
                response.getMessage());
    }

    private static String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null || (timestamp.getSeconds() == 0 && timestamp.getNanos() == 0)) {
            return null;
        }
        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
        return ISO_FORMAT.format(instant.atOffset(ZoneOffset.UTC));
    }
}
