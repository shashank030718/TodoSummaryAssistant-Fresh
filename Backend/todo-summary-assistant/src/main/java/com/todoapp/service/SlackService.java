
package com.todoapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SlackService {

    @Value("${slack.webhook.url:}")
    private String slackWebhookUrl;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SlackService() {
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public boolean sendSlackMessage(String message) {

        if (slackWebhookUrl == null
                || slackWebhookUrl.isBlank()
                || !slackWebhookUrl.startsWith(
                        "https://hooks.slack.com/services/")) {
            System.out.println(
                    "Slack webhook is not configured. Skipping notification."
            );
            return false;
        }

        ObjectNode json = objectMapper.createObjectNode();
        json.put("text", message);

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(slackWebhookUrl)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println(
                        "Slack notification failed. HTTP status: "
                                + response.code()
                );
            }
            return response.isSuccessful();
        } catch (IOException e) {
            System.err.println(
                    "Could not send Slack notification: " + e.getMessage()
            );
            return false;
        }
    }
}
