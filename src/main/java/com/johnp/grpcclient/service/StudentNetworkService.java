package com.johnp.grpcclient.service;

import com.johnp.grpc.StudentProfileRequest;
import com.johnp.grpc.StudentProfileResponse;
import com.johnp.grpc.StudentsServiceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StudentNetworkService {

    private final StudentsServiceGrpc.StudentsServiceBlockingStub studentsServiceBlockingStub;

    @Autowired
    public StudentNetworkService(StudentsServiceGrpc.StudentsServiceBlockingStub studentsServiceBlockingStub) {
        this.studentsServiceBlockingStub = studentsServiceBlockingStub;
    }

    public StudentProfileResponse getStudentProfile(int studentId) {
        StudentProfileRequest request = StudentProfileRequest.newBuilder()
                .setStudentId(studentId)
                .build();
        return studentsServiceBlockingStub.getStudentProfile(request);
    }
}
