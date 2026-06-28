package com.johnp.grpcclient.controller;

import com.johnp.grpcclient.dto.*;
import com.johnp.grpcclient.gateway.StudentGrpcGateway;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * REST + SSE endpoints for the gRPC demo BFF.
 * <p>
 * Bidirectional streaming is <b>not</b> exposed here — WebSocket cannot use {@code @GetMapping}.
 * See {@link com.johnp.grpcclient.config.WebSocketConfig} and
 * {@link com.johnp.grpcclient.websocket.AdvisingWebSocketHandler} for the bidi endpoint
 * at {@code /ws/demo/advising}.
 */
@RestController
@RequestMapping("/api/demo")
public class StudentDemoController {

    private static final Logger log = LoggerFactory.getLogger(StudentDemoController.class);

    private final StudentGrpcGateway studentGrpcGateway;

    public StudentDemoController(StudentGrpcGateway studentGrpcGateway) {
        this.studentGrpcGateway = studentGrpcGateway;
    }

    @GetMapping
    public DemoApiInfoDto info() {
        return new DemoApiInfoDto(
                "Students gRPC Demo BFF",
                "HTTP bridge for Angular UI → gRPC server on port 9090",
                List.of(
                        new DemoEndpointInfoDto(
                                "UNARY",
                                "HTTP",
                                "GET",
                                "/api/demo/students/{id}",
                                "Fetch a single student profile"),
                        new DemoEndpointInfoDto(
                                "SERVER_STREAM",
                                "SSE",
                                "GET",
                                "/api/demo/courses/stream",
                                "Stream course catalog (Server-Sent Events)"),
                        new DemoEndpointInfoDto(
                                "CLIENT_STREAM",
                                "HTTP",
                                "POST",
                                "/api/demo/enrollments/batch",
                                "Send a batch of enrollments, receive one summary response"),
                        new DemoEndpointInfoDto(
                                "BIDI_STREAM",
                                "WEBSOCKET",
                                null,
                                "/ws/demo/advising",
                                "Live enrollment advising — see GET /api/demo/advising for connection details")
                ));
    }

    /**
     * Discovery endpoint for the bidirectional WebSocket stream.
     * The actual stream is handled by {@link com.johnp.grpcclient.websocket.AdvisingWebSocketHandler}.
     */
    @GetMapping("/advising")
    public AdvisingWebSocketInfoDto advisingInfo(HttpServletRequest request) {
        String wsUrl = "ws://" + request.getServerName() + ":" + request.getServerPort() + "/ws/demo/advising";
        return new AdvisingWebSocketInfoDto(
                "WEBSOCKET",
                "/ws/demo/advising",
                wsUrl,
                "liveEnrollmentAdvising",
                "{\"studentId\":1,\"courseId\":1,\"term\":\"Fall 2024\",\"status\":\"Enrolled\",\"grade\":3.5}",
                "{\"type\":\"FEEDBACK\",\"payload\":{\"status\":\"APPROVED\",\"message\":\"...\", ...}}",
                "CLOSE");
    }

    @GetMapping("/students/{studentId}")
    public StudentProfileDto getStudentProfile(@PathVariable long studentId) {
        try {
            return studentGrpcGateway.getStudentProfile(studentId);
        } catch (StatusRuntimeException ex) {
            throw mapGrpcException(ex);
        }
    }

    @GetMapping("/catalog/students")
    public List<StudentSummaryDto> listStudents() {
        try {
            return studentGrpcGateway.listStudentCatalog();
        } catch (StatusRuntimeException ex) {
            throw mapGrpcException(ex);
        }
    }

    @GetMapping("/catalog/courses")
    public List<CourseSummaryDto> listCourses() {
        try {
            return studentGrpcGateway.listCourseCatalog();
        } catch (StatusRuntimeException ex) {
            throw mapGrpcException(ex);
        }
    }

    @GetMapping(value = "/courses/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamCourseCatalog() {
        SseEmitter emitter = new SseEmitter(120_000L);
        try {
            studentGrpcGateway.streamCourseCatalog(emitter);
        } catch (Exception ex) {
            log.error("Failed to start course catalog stream", ex);
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    @PostMapping("/enrollments/batch")
    public BatchEnrollResponseDto batchEnroll(@RequestBody List<EnrollmentRequestDto> enrollments) {
        if (enrollments == null || enrollments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one enrollment is required");
        }
        try {
            return studentGrpcGateway.batchEnroll(enrollments);
        } catch (StatusRuntimeException ex) {
            throw mapGrpcException(ex);
        }
    }

    private ResponseStatusException mapGrpcException(StatusRuntimeException ex) {
        Status.Code code = ex.getStatus().getCode();
        String description = ex.getStatus().getDescription() != null
                ? ex.getStatus().getDescription()
                : ex.getMessage();

        HttpStatus httpStatus = switch (code) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        return new ResponseStatusException(httpStatus, description, ex);
    }
}
