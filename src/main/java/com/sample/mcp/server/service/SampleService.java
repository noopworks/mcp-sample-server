package com.sample.mcp.server.service;

import com.sample.mcp.server.tool.SampleTool.SampleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class SampleService {

    public SampleResponse findById(String entityId) {
        log.debug("Finding entity by id={}", entityId);
        return new SampleResponse(entityId, "Sample Entity", "ACTIVE", "demo,template");
    }

    public SampleResponse create(String name, String tags) {
        String generatedId = UUID.randomUUID().toString();
        log.debug("Creating entity name={}, id={}", name, generatedId);
        return new SampleResponse(generatedId, name, "CREATED", tags);
    }
}
