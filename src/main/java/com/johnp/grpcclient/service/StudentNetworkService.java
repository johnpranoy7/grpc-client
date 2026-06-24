package com.johnp.grpcclient.service;

import com.google.protobuf.Empty;
import com.johnp.grpc.*;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class StudentNetworkService {

    private final StudentsServiceGrpc.StudentsServiceBlockingStub studentsServiceBlockingStub;
    private final StudentsServiceGrpc.StudentsServiceStub studentsServiceStub;

    @Autowired
    public StudentNetworkService(
            StudentsServiceGrpc.StudentsServiceBlockingStub studentsServiceBlockingStub,
            StudentsServiceGrpc.StudentsServiceStub studentsServiceStub) {
        this.studentsServiceBlockingStub = studentsServiceBlockingStub;
        this.studentsServiceStub = studentsServiceStub;
    }

    public void getStudentProfile(int studentId) {
        StudentProfileRequest request = StudentProfileRequest.newBuilder()
                .setStudentId(studentId)
                .build();
        StudentProfileResponse studentProfile = studentsServiceBlockingStub.getStudentProfile(request);
        System.out.println("Student Profile: " + studentProfile);
    }

    public void subscribeCourseCatalog() {
        CountDownLatch latch = new CountDownLatch(1);

        studentsServiceStub.streamCourseCatalog(Empty.newBuilder().build(), new StreamObserver<CourseSummary>() {
            @Override
            public void onNext(CourseSummary courseSummary) {
                System.out.println("Course Summary: " + courseSummary);
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error occurred while streaming course catalog: " + t.getMessage());
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("Finished streaming course catalog.");
                latch.countDown();
            }
        });

        awaitResponse(latch, "course catalog stream");
    }

    public void publishBatchEnrollments(List<StudentEnrollmentRequest> enrollmentList) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        StreamObserver<StudentEnrollmentRequest> requestObserver =
                studentsServiceStub.batchConsumeEnrollStudents(new StreamObserver<BatchEnrollStudentsResponse>() {
                    @Override
                    public void onNext(BatchEnrollStudentsResponse response) {
                        System.out.println("Batch Enrollment Response: " + response);
                        System.out.println("Successfully enrolled " + response.getSuccessCount() + " students.");
                        System.out.println("Failed to enroll " + response.getFailureCount() + " students.");
                        System.out.println("Failed Student IDs: " + response.getFailedStudentIdsList());
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.err.println("Error occurred while sending batch enrollment requests: "
                                + throwable.getMessage());
                        error.set(throwable);
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("Finished receiving batch enrollment response.");
                        latch.countDown();
                    }
                });

        for (StudentEnrollmentRequest enrollmentRequest : enrollmentList) {
            requestObserver.onNext(enrollmentRequest);
        }
        requestObserver.onCompleted();

        awaitResponse(latch, "batch enrollment");
        if (error.get() != null) {
            throw new RuntimeException("Batch enrollment failed", error.get());
        }
    }

    private void awaitResponse(CountDownLatch latch, String operation) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timed out waiting for " + operation + " response");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for " + operation + " response", e);
        }
    }
}
