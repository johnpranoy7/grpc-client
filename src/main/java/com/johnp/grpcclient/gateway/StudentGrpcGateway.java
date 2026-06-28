package com.johnp.grpcclient.gateway;

import com.google.protobuf.Empty;
import com.johnp.grpc.*;
import com.johnp.grpcclient.dto.*;
import com.johnp.grpcclient.mapper.ProtoMapper;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Service
public class StudentGrpcGateway {

    private static final Logger log = LoggerFactory.getLogger(StudentGrpcGateway.class);
    private static final long STREAM_TIMEOUT_MS = 120_000L;

    private final StudentsServiceGrpc.StudentsServiceBlockingStub blockingStub;
    private final StudentsServiceGrpc.StudentsServiceStub asyncStub;

    public StudentGrpcGateway(
            StudentsServiceGrpc.StudentsServiceBlockingStub blockingStub,
            StudentsServiceGrpc.StudentsServiceStub asyncStub) {
        this.blockingStub = blockingStub;
        this.asyncStub = asyncStub;
    }

    public StudentProfileDto getStudentProfile(long studentId) {
        log.info("Unary gRPC call: getStudentProfile studentId={}", studentId);
        StudentProfileRequest request = StudentProfileRequest.newBuilder()
                .setStudentId(studentId)
                .build();
        StudentProfileResponse response = blockingStub.getStudentProfile(request);
        return ProtoMapper.toDto(response);
    }

    public void streamCourseCatalog(SseEmitter emitter) {
        log.info("Server-streaming gRPC call: streamCourseCatalog");
        asyncStub.streamCourseCatalog(Empty.getDefaultInstance(), new StreamObserver<CourseSummary>() {
            @Override
            public void onNext(CourseSummary courseSummary) {
                try {
                    CourseSummaryDto dto = ProtoMapper.toDto(courseSummary);
                    emitter.send(SseEmitter.event()
                            .name("course")
                            .data(dto));
                    log.info("Streamed course: courseId={}, name={}", dto.courseId(), dto.courseName());
                } catch (IOException ex) {
                    log.error("Failed to send SSE course event", ex);
                    emitter.completeWithError(ex);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Course catalog stream error", throwable);
                emitter.completeWithError(throwable);
            }

            @Override
            public void onCompleted() {
                log.info("Course catalog stream completed");
                emitter.complete();
            }
        });

        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> log.error("SSE connection error", error));
    }

    public BatchEnrollResponseDto batchEnroll(List<EnrollmentRequestDto> enrollments) {
        log.info("Client-streaming gRPC call: batchConsumeEnrollStudents count={}", enrollments.size());
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<BatchEnrollResponseDto> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        StreamObserver<StudentEnrollmentRequest> requestObserver =
                asyncStub.batchConsumeEnrollStudents(new StreamObserver<BatchEnrollStudentsResponse>() {
                    @Override
                    public void onNext(BatchEnrollStudentsResponse response) {
                        result.set(ProtoMapper.toDto(response));
                        log.info("Batch enroll result: success={}, failed={}",
                                response.getSuccessCount(), response.getFailureCount());
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        log.error("Batch enroll stream error", throwable);
                        error.set(throwable);
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }
                });

        for (EnrollmentRequestDto enrollment : enrollments) {
            requestObserver.onNext(ProtoMapper.toProto(enrollment));
        }
        requestObserver.onCompleted();

        awaitLatch(latch, "batch enrollment");
        if (error.get() != null) {
            throw new RuntimeException("Batch enrollment failed", error.get());
        }
        return result.get();
    }

    public StreamObserver<EnrollmentIntent> openAdvisingSession(
            Consumer<EnrollmentFeedbackDto> onFeedback,
            Runnable onCompleted,
            Consumer<Throwable> onError) {
        log.info("Bidirectional gRPC call: liveEnrollmentAdvising session opened");

        StreamObserver<EnrollmentFeedback> feedbackObserver = new StreamObserver<>() {
            @Override
            public void onNext(EnrollmentFeedback feedback) {
                EnrollmentFeedbackDto dto = ProtoMapper.toDto(feedback);
                log.info("Advising feedback: status={}, studentId={}, courseId={}",
                        dto.status(), dto.studentId(), dto.courseId());
                onFeedback.accept(dto);
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Advising session error", throwable);
                onError.accept(throwable);
            }

            @Override
            public void onCompleted() {
                log.info("Advising session completed");
                onCompleted.run();
            }
        };

        return asyncStub.liveEnrollmentAdvising(feedbackObserver);
    }

    public void sendAdvisingIntent(StreamObserver<EnrollmentIntent> intentObserver, EnrollmentIntentDto intent) {
        log.info("Sending advising intent: studentId={}, courseId={}, term={}",
                intent.studentId(), intent.courseId(), intent.term());
        intentObserver.onNext(ProtoMapper.toProto(intent));
    }

    public void closeAdvisingSession(StreamObserver<EnrollmentIntent> intentObserver) {
        log.info("Closing advising session");
        intentObserver.onCompleted();
    }

    private void awaitLatch(CountDownLatch latch, String operation) {
        try {
            if (!latch.await(STREAM_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Timed out waiting for " + operation);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for " + operation, ex);
        }
    }
}
