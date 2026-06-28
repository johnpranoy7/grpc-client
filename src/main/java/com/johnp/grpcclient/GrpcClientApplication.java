package com.johnp.grpcclient;

import com.johnp.grpc.StudentsServiceGrpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.grpc.client.ImportGrpcClients;

@SpringBootApplication
@ImportGrpcClients(
        target = "studentService",
        types = {
                StudentsServiceGrpc.StudentsServiceBlockingStub.class,
                StudentsServiceGrpc.StudentsServiceStub.class
        })
public class GrpcClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrpcClientApplication.class, args);
    }
}
