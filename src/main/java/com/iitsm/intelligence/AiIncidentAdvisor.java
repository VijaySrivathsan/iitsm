package com.iitsm.intelligence;

import com.iitsm.domain.incident.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Intelligent feature - External AI / Google Gemini API integration for incident triage,
 * automated root-cause analysis, and recommended investigation steps.
 *
 * <p>Uses Java 21's native {@link HttpClient}. If an API key is configured
 * ({@code itsm.ai.gemini-api-key}), it queries the Gemini REST API; otherwise, it seamlessly
 * falls back to an expert rule-based heuristic advisor so the application remains 100% functional
 * offline or in local demo mode.
 */
@Service
public class AiIncidentAdvisor {

    private static final Logger log = LoggerFactory.getLogger(AiIncidentAdvisor.class);

    private final String apiKey;
    private final String model;

    public AiIncidentAdvisor(@Value("${itsm.ai.gemini-api-key:}") String apiKey,
                             @Value("${itsm.ai.model:gemini-1.5-flash}") String model) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null ? model.trim() : "gemini-1.5-flash";
    }

    public AiAdvice analyze(Incident incident) {
        if (!apiKey.isEmpty()) {
            try {
                return callGemini(incident);
            } catch (Throwable e) {
                log.warn("Gemini API call failed, falling back to local heuristic: {}", e.getMessage());
            }
        }
        return localHeuristic(incident);
    }

    private AiAdvice callGemini(Incident incident) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model
                + ":generateContent?key=" + apiKey;

        String prompt = "You are an expert IT Service Management (ITSM) engineer. Analyze this incident:\n"
                + "Title: " + incident.getTitle() + "\n"
                + "Description: " + incident.getDescription() + "\n"
                + "Service: " + incident.getService().getName() + " (Criticality: " + incident.getService().getBusinessCriticality() + "/5)\n"
                + "Priority: " + priorityLabel(incident) + "\n"
                + "Affected Users: " + incident.getAffectedUsers() + "\n"
                + "Provide concise advice: 1) Executive Summary, 2) Recommended Actions, 3) Probable Root Cause.";

        String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]}]}";

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            String body = response.body();
            String text = extractTextFromGemini(body);
            if (text != null && !text.isBlank()) {
                return new AiAdvice(
                        "AI Advisor Analysis (" + model + ")",
                        text,
                        List.of("Review service health logs", "Verify database connection pool", "Inspect recent infrastructure alerts"),
                        true
                );
            }
        }
        return localHeuristic(incident);
    }

    private AiAdvice localHeuristic(Incident incident) {
        String service = incident.getService().getName();
        int users = incident.getAffectedUsers();

        String summary = incident.getPriority() == null
                ? String.format("Automated triage for %s on '%s'. Incident is not yet prioritised (awaiting classification); ~%d users affected.",
                        incident.getNumber(), service, users)
                : String.format("Automated triage for %s on '%s'. Priority score indicates %s severity impacting ~%d users.",
                        incident.getNumber(), service, priorityLabel(incident), users);

        List<String> steps = new ArrayList<>();
        steps.add("Check health and uptime telemetry for " + service);
        if (service.toLowerCase().contains("payment") || service.toLowerCase().contains("gateway")) {
            steps.add("Verify payment gateway upstream webhook callbacks and SSL certificate status");
            steps.add("Check for transaction timeouts in core database connection pool");
        } else if (service.toLowerCase().contains("auth") || service.toLowerCase().contains("sso")) {
            steps.add("Check LDAP / OAuth identity provider token signing service");
            steps.add("Verify Redis session cache cluster connectivity");
        } else if (service.toLowerCase().contains("database")) {
            steps.add("Inspect active SQL query locks and replication lag");
            steps.add("Verify disk IOPS and memory consumption on primary node");
        } else {
            steps.add("Check recent deployments or configuration changes in the last 2 hours");
            steps.add("Inspect infrastructure logs for CPU/memory throttling");
        }
        steps.add("Notify team lead if resolution exceeds 75% of SLA resolution window");

        String rootCause = "Probable cause: Resource exhaustion or transient dependency failure on upstream " + service + " components.";

        return new AiAdvice(summary, rootCause, steps, false);
    }

    /** Incidents have no priority until an admin classifies them (status NEW). */
    private static String priorityLabel(Incident incident) {
        return incident.getPriority() == null ? "not yet classified" : incident.getPriority().display();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private static String extractTextFromGemini(String json) {
        int idx = json.indexOf("\"text\": \"");
        if (idx != -1) {
            int start = idx + 9;
            int end = json.indexOf("\"", start);
            if (end != -1) {
                return json.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"");
            }
        }
        return null;
    }

    public record AiAdvice(String summary, String rootCauseHypothesis, List<String> investigationSteps, boolean isRemoteAi) {}
}
