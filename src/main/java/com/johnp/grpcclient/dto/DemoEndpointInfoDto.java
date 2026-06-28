package com.johnp.grpcclient.dto;

public record DemoEndpointInfoDto(
        String grpcType,
        String protocol,
        String httpMethod,
        String path,
        String description
) {
}
