package com.johnp.grpcclient;

import com.johnp.grpc.StudentEnrollmentRequest;
import com.johnp.grpc.StudentsServiceGrpc;
import com.johnp.grpcclient.service.StudentNetworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.grpc.client.ImportGrpcClients;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@ImportGrpcClients(target = "studentService",
        types = {StudentsServiceGrpc.StudentsServiceBlockingStub.class, StudentsServiceGrpc.StudentsServiceStub.class})
public class GrpcClientApplication implements CommandLineRunner {

    @Autowired
    private StudentNetworkService studentNetworkService;

    public static void main(String[] args) {
        SpringApplication.run(GrpcClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
//        studentNetworkService.getStudentProfile(1);
//        studentNetworkService.subscribeCourseCatalog();

        StudentEnrollmentRequest enrollment1 = StudentEnrollmentRequest.newBuilder().setStudentId(2)
                .setCourseId(1).setTerm("Fall 2024")
                .setStatus("Enrolled").setGrade(2.2).build();

        StudentEnrollmentRequest enrollment2 = StudentEnrollmentRequest.newBuilder().setStudentId(2)
                .setCourseId(2).setTerm("Fall 2024")
                .setStatus("In Progress").setGrade(2.5).build();
        StudentEnrollmentRequest enrollment3 = StudentEnrollmentRequest.newBuilder().setStudentId(2)
                .setCourseId(2).setTerm("Spring 2024")
                .setStatus("In Progress").setGrade(2.5).build();

        List<StudentEnrollmentRequest> enrollmentList = new ArrayList<>();
        enrollmentList.add(enrollment1);
        enrollmentList.add(enrollment2);
        enrollmentList.add(enrollment3);

        studentNetworkService.publishBatchEnrollments(enrollmentList);
    }
}
