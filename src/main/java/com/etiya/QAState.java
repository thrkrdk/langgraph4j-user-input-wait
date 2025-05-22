package com.etiya;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;
import java.util.Optional;

public class QAState extends AgentState {
    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            "question", Channels.base(() -> ""),
            "answer", Channels.base(() -> ""),
            "user_feedback", Channels.base(() -> ""),
            "messages", Channels.base(() -> "")
    );

    public QAState() {
        super(Map.of(
                "question", "",
                "answer", "",
                "user_feedback", "",
                "messages", ""
        ));
    }

    public QAState(Map<String, Object> data) {
        super(data);
    }

    public String question() {
        return this.<String>value("question").orElse("");
    }


    public String answer() {
        return this.<String>value("answer").orElse("");
    }

    public Optional<String> userFeedback() {
        return value("user_feedback").map(s -> s.toString().isEmpty() ? null : s.toString());
    }

    public String messages() {
        return this.<String>value("messages").orElse("");
    }
}