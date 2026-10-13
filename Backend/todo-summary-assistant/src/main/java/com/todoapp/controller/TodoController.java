
package com.todoapp.controller;

import com.todoapp.entity.Todo;
import com.todoapp.service.CohereService;
import com.todoapp.service.SlackService;
import com.todoapp.service.TodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/todos")
public class TodoController {

    @Autowired
    private TodoService todoService;

    @Autowired
    private CohereService cohereService;

    @Autowired
    private SlackService slackService;

    @GetMapping
    public ResponseEntity<List<Todo>> getAllTodos() {
        return ResponseEntity.ok(todoService.getAllTodos());
    }

    @PostMapping
    public ResponseEntity<Todo> createTodo(@RequestBody Todo todo) {
        return new ResponseEntity<>(
                todoService.createTodo(todo),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Todo> updateTodo(
            @PathVariable Long id,
            @RequestBody Todo todoDetails) {
        return ResponseEntity.ok(
                todoService.updateTodo(id, todoDetails)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(@PathVariable Long id) {
        todoService.deleteTodo(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/summarize")
    public ResponseEntity<Map<String, String>> summarizeAndSendToSlack() {
        try {
            List<Todo> pendingTodos = todoService.getPendingTodos();

            if (pendingTodos.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "summary", "No pending todos to summarize.",
                        "notification", "not_sent"
                ));
            }

            String todosText = pendingTodos.stream()
                    .map(todo -> "- " + todo.getTitle()
                            + ": " + todo.getDescription())
                    .collect(Collectors.joining("\n"));

            String summary = cohereService.summarizeText(
                    "Please summarize the following to-do items:\n"
                            + todosText
            );

            boolean slackSuccess = slackService.sendSlackMessage(
                    "Todo Summary:\n" + summary
            );

            String notificationStatus = slackSuccess
                    ? "sent"
                    : "not_sent";

            return ResponseEntity.ok(Map.of(
                    "summary", summary,
                    "notification", notificationStatus,
                    "message", slackSuccess
                            ? "Summary generated and sent to Slack."
                            : "Summary generated. Slack notification was skipped or failed."
            ));

        } catch (Exception e) {
            System.err.println(
                    "Error generating todo summary: " + e.getMessage()
            );

            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR
            ).body(Map.of(
                    "error", "Failed to generate summary. Please try again."
            ));
        }
    }
}
