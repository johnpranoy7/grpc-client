package com.johnp.grpcclient.dto;

import java.util.List;

public record DemoApiInfoDto(
        String name,
        String description,
        List<DemoEndpointInfoDto> endpoints
) {
}
