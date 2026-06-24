package com.johnp.grpcclient.service;

import com.google.protobuf.Empty;
import com.johnp.grpc.CourseSummary;
import com.johnp.grpc.StudentProfileRequest;
import com.johnp.grpc.StudentProfileResponse;
import com.johnp.grpc.StudentsServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
}
