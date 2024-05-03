package com.cashwu.vertexai.controller;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author cash.wu
 * @since 2024/05/03
 */
@RestController
public class LangChainController {

    private final String modelName = "gemini-1.0-pro";
    private final String prompt = "隨便跟我說一個笑話，使用正體中文回答";

    @GetMapping("/langchain/generate")
    public String generate() {

        System.out.println("project id : " + System.getenv("PROJECT_ID"));
        System.out.println("location : " + System.getenv("LOCATION"));
        ChatLanguageModel model = VertexAiGeminiChatModel.builder()
                                                         .project(System.getenv("PROJECT_ID"))
                                                         .location(System.getenv("LOCATION"))
                                                         .modelName("gemini-1.0-pro")
                                                         .build();

        return model.generate(prompt);
    }
}
