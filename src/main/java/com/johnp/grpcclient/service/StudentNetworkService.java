package com.johnp.grpcclient.service;

import com.google.protobuf.Empty;
import com.johnp.grpc.*;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentNetworkService {

    //For Unary (REST Type Call)
    private final StudentsServiceGrpc.StudentsServiceBlockingStub studentsServiceBlockingStub;

    //For Streaming (ClientStreaming, ServerStreaming, BidirectionalStreaming)
    private final StudentsServiceGrpc.StudentsServiceStub studentsServiceStub;

    @Autowired
    public StudentNetworkService(StudentsServiceGrpc.StudentsServiceBlockingStub studentsServiceBlockingStub, StudentsServiceGrpc.StudentsServiceStub studentsServiceStub) {
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
        studentsServiceStub.streamCourseCatalog(Empty.newBuilder().build(), new StreamObserver<CourseSummary>() {
            @Override
            public void onNext(CourseSummary courseSummary) {
                System.out.println("Course Summary: " + courseSummary);
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error occurred while streaming course catalog: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Finished streaming course catalog.");
            }
        });
    }

    public void publishBatchEnrollments(List<StudentEnrollmentRequest> enrollmentList) {

        StreamObserver<StudentEnrollmentRequest> studentEnrollmentRequestStreamObserver = studentsServiceStub.batchConsumeEnrollStudents(new StreamObserver<BatchEnrollStudentsResponse>() {
            @Override
            public void onNext(BatchEnrollStudentsResponse response) {
                System.out.println("Batch Enrollment Response: " + response);
                System.out.println("Successfully enrolled " + response.getSuccessCount() + " students.");
                System.out.println("Failed to enroll " + response.getFailureCount() + " students.");
                System.out.println("Failed Student IDs: " + response.getFailedStudentIdsList());
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error occurred while sending batch enrollment requests: " + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Finished sending batch enrollment requests.");
            }
        });

        try {
            for (StudentEnrollmentRequest studentEnrollmentRequest : enrollmentList) {
                studentEnrollmentRequestStreamObserver.onNext(studentEnrollmentRequest);
            }
            studentEnrollmentRequestStreamObserver.onCompleted();
        } catch (Exception e) {
            studentEnrollmentRequestStreamObserver.onError(e);
        }
    }
}
