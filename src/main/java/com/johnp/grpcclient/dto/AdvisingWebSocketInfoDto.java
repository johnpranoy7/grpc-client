package com.johnp.grpcclient.dto;

public record AdvisingWebSocketInfoDto(
        String protocol,
        String path,
        String fullUrl,
        String grpcRpc,
        String clientSendFormat,
        String serverMessageFormat,
        String closeCommand
) {
}
