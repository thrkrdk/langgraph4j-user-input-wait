package com.etiya;

import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.checkpoint.MemorySaver;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

public class QAAssistant {

    private final CompiledGraph<QAState> graph;
    private final String threadId;

    public QAAssistant() throws GraphStateException {
        // Düğümleri tanımla
        AsyncNodeAction<QAState> processQuestion = node_async(state -> {
            String question = state.question();
            // Gerçek uygulamada burada bir LLM çağrısı yapılabilir
            String answer = "Bu sorunun cevabı: " + question;

            return Map.of(
                    "messages", "Sistem: Sorunuzu işliyorum...",
                    "answer", answer
            );
        });

        AsyncNodeAction<QAState> showAnswer = node_async(state -> {
            return Map.of(
                    "messages", "Sistem: " + state.answer()
            );
        });

        AsyncNodeAction<QAState> waitForFeedback = node_async(state -> {
            // Bu düğüm sadece kullanıcı girdisini bekler, bir şey yapmaz
            return Map.of();
        });

        AsyncNodeAction<QAState> processFeedback = node_async(state -> {
            String feedback = state.userFeedback().orElse("unknown");

            if (feedback.equals("evet")) {
                return Map.of(
                        "messages", "Süper!"
                );
            } else {
                String improvedAnswer = state.answer() + " (yeni cevap cevap)";
                return Map.of(
                        "messages", "Sistem: daha iyi cevap verdim: " + improvedAnswer,
                        "answer", improvedAnswer
                );
            }
        });

        // Kullanıcı geri bildirimini değerlendiren kenar
        AsyncEdgeAction<QAState> evaluateFeedback = edge_async(state -> {
            String feedback = state.userFeedback().orElse("unknown");

            if (feedback.equals("evet")) {
                return "satisfied";
            } else if (feedback.equals("hayir")) {
                return "not_satisfied";
            } else {
                return "unknown";
            }
        });

        // Grafiği oluştur
        var builder = new StateGraph<>(QAState.SCHEMA, QAState::new)
                .addNode("process_question", processQuestion)
                .addNode("show_answer", showAnswer)
                .addNode("wait_for_feedback", waitForFeedback)
                .addNode("process_feedback", processFeedback)

                // Kenarları tanımla
                .addEdge(START, "process_question")
                .addEdge("process_question", "show_answer")
                .addEdge("show_answer", "wait_for_feedback")

                // Koşullu kenarlar
                .addConditionalEdges("wait_for_feedback", evaluateFeedback,
                        Map.of(
                                "satisfied", END,
                                "not_satisfied", "process_feedback",
                                "unknown", "wait_for_feedback"
                        ))
                .addEdge("process_feedback", END);

        // Bellek ayarla
        MemorySaver saver = new MemorySaver();

        // Kesme noktasını tanımla
        var compileConfig = CompileConfig.builder()
                .checkpointSaver(saver)
                .interruptBefore("wait_for_feedback") // Kullanıcı geri bildirimi beklemeden önce duraklat
                .build();

        // Grafiği derle
        this.graph = builder.compile(compileConfig);
        this.threadId = "conversation-" + System.currentTimeMillis();
    }

    public void startConversation(String question) {
        // Başlangıç girdisi
        Map<String, Object> initialInput = Map.of(
                "question", question,
                "messages", "Kullanıcı: " + question
        );

        // Thread ID tanımla
        var invokeConfig = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        // Grafiği ilk kesme noktasına kadar çalıştır
        System.out.println("=== İlk Çalıştırma ===");
        for (var event : graph.stream(initialInput, invokeConfig)) {
            System.out.println(event);
        }

        // Mevcut durumu kontrol et
        System.out.println("\n=== Mevcut Durum ===");
        System.out.println(graph.getState(invokeConfig));
    }

    public void provideFeedback(String feedback) throws Exception {
        // Thread ID tanımla
        var invokeConfig = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        System.out.println("\n=== Kullanıcı Geri Bildirimi ===");
        System.out.println("Kullanıcı: " + feedback);

        // Durumu güncelle
        var updateConfig = graph.updateState(
                invokeConfig,
                Map.of("user_feedback", feedback, "messages", "Kullanıcı: " + feedback),
                null
        );

        // Grafiği devam ettir
        System.out.println("\n=== Devam Eden Çalıştırma ===");
        for (var event : graph.stream(null, updateConfig)) {
            System.out.println(event);
        }

        // Son durumu kontrol et
        System.out.println("\n=== Son Durum ===");
        System.out.println(graph.getState(updateConfig));
    }
}