package com.johnp.grpcclient.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnp.grpc.EnrollmentIntent;
import com.johnp.grpcclient.dto.EnrollmentFeedbackDto;
import com.johnp.grpcclient.dto.EnrollmentIntentDto;
import com.johnp.grpcclient.gateway.StudentGrpcGateway;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for the bidirectional {@code liveEnrollmentAdvising} gRPC RPC.
 * <p>
 * Browser connects to {@code ws://localhost:8080/ws/demo/advising}, sends JSON intents,
 * and receives JSON feedback messages in real time.
 */
@Component
public class AdvisingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AdvisingWebSocketHandler.class);

    private final StudentGrpcGateway studentGrpcGateway;
    private final ObjectMapper objectMapper;
    private final Map<String, StreamObserver<EnrollmentIntent>> sessions = new ConcurrentHashMap<>();

    public AdvisingWebSocketHandler(StudentGrpcGateway studentGrpcGateway, ObjectMapper objectMapper) {
        this.studentGrpcGateway = studentGrpcGateway;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        StreamObserver<EnrollmentIntent> intentObserver = studentGrpcGateway.openAdvisingSession(
                feedback -> sendFeedback(session, feedback),
                () -> closeSession(session, CloseStatus.NORMAL),
                error -> closeSessionWithError(session, error));

        sessions.put(session.getId(), intentObserver);
        sendControlMessage(session, Map.of(
                "type", "SESSION_STARTED",
                "message", "Advising session ready. Send enrollment intents as JSON."));
        log.info("WebSocket advising session established: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        StreamObserver<EnrollmentIntent> intentObserver = sessions.get(session.getId());
        if (intentObserver == null) {
            sendControlMessage(session, Map.of("type", "ERROR", "message", "Session not initialized"));
            return;
        }

        String payload = message.getPayload().trim();
        if ("CLOSE".equalsIgnoreCase(payload)) {
            studentGrpcGateway.closeAdvisingSession(intentObserver);
            sessions.remove(session.getId());
            return;
        }

        EnrollmentIntentDto intent = objectMapper.readValue(payload, EnrollmentIntentDto.class);
        studentGrpcGateway.sendAdvisingIntent(intentObserver, intent);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        StreamObserver<EnrollmentIntent> intentObserver = sessions.remove(session.getId());
        if (intentObserver != null) {
            try {
                intentObserver.onCompleted();
            } catch (Exception ex) {
                log.warn("Error completing gRPC stream on WebSocket close", ex);
            }
        }
        log.info("WebSocket advising session closed: {} status={}", session.getId(), status);
    }

    private void sendFeedback(WebSocketSession session, EnrollmentFeedbackDto feedback) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                        Map.of("type", "FEEDBACK", "payload", feedback))));
            }
        } catch (IOException ex) {
            log.error("Failed to send advising feedback over WebSocket", ex);
        }
    }

    private void sendControlMessage(WebSocketSession session, Map<String, String> message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            }
        } catch (IOException ex) {
            log.error("Failed to send WebSocket control message", ex);
        }
    }

    private void closeSession(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException ex) {
            log.warn("Failed to close WebSocket session", ex);
        }
    }

    private void closeSessionWithError(WebSocketSession session, Throwable error) {
        sendControlMessage(session, Map.of(
                "type", "ERROR",
                "message", error.getMessage() != null ? error.getMessage() : "Advising stream failed"));
        closeSession(session, CloseStatus.SERVER_ERROR);
    }
}
