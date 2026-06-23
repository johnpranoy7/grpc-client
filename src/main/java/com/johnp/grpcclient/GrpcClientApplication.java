package com.johnp.grpcclient;

import com.johnp.grpc.StudentProfileResponse;
import com.johnp.grpc.StudentsServiceGrpc;
import com.johnp.grpcclient.service.StudentNetworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.grpc.client.ImportGrpcClients;

@SpringBootApplication
@ImportGrpcClients(target = "studentService", types = StudentsServiceGrpc.StudentsServiceBlockingStub.class)
public class GrpcClientApplication implements CommandLineRunner {

    @Autowired
    private StudentNetworkService studentNetworkService;

    public static void main(String[] args) {
        SpringApplication.run(GrpcClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        StudentProfileResponse studentProfile = studentNetworkService.getStudentProfile(1);
        System.out.println("Student Profile: " + studentProfile);
    }
}
