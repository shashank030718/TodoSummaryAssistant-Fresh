package com.todoapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class CohereService {

    @Value("${cohere.api.key}")
    private String cohereApiKey;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CohereService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        this.objectMapper = new ObjectMapper();
    }

    public String summarizeText(String text) throws IOException {

        String url = "https://api.cohere.ai/v2/chat";

        ObjectNode requestBodyJson = objectMapper.createObjectNode();
        requestBodyJson.put("model", "command-a-plus-05-2026");

        ArrayNode messages = requestBodyJson.putArray("messages");

        ObjectNode message = messages.addObject();
        message.put("role", "user");
        message.put(
                "content",
                "Summarize the following pending todo tasks into a clear, "
                        + "concise paragraph. Mention the main tasks and priorities. "
                        + "Do not invent details.\n\n"
                        + text
        );

        RequestBody body = RequestBody.create(
                requestBodyJson.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + cohereApiKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {

            ResponseBody responseBody = response.body();
            String responseText = responseBody != null
                    ? responseBody.string()
                    : "";

            if (!response.isSuccessful()) {
                throw new IOException(
                        "Cohere API error: HTTP "
                                + response.code() + " - " + responseText
                );
            }

            JsonNode jsonNode = objectMapper.readTree(responseText);

            JsonNode content = jsonNode.path("message").path("content");

            if (content.isArray()) {
                for (JsonNode item : content) {
                    if ("text".equals(item.path("type").asText())) {
                        String summary = item.path("text").asText("");

                        if (!summary.isBlank()) {
                            return summary;
                        }
                    }
                }
            }

            throw new IOException(
                    "Cohere response did not contain a text summary."
            );  
        }
    }
}