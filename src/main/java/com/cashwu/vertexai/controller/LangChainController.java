package com.cashwu.vertexai.controller;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.vertexai.VertexAiChatModel;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.model.vertexai.VertexAiImageModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.file.Path;
import java.util.Base64;

/**
 * @author cash.wu
 * @since 2024/05/03
 */
@RestController
public class LangChainController {

    private final String prompt = "隨便跟我說一個笑話，使用正體中文回答";
    private final String projectId = System.getenv("PROJECT_ID")
                                           .trim();
    private final String location = System.getenv("LOCATION")
                                          .trim();
    private final String endpoint = String.format("%s-aiplatform.googleapis.com:443", location);

    @GetMapping("/langchain/generate")
    public String generate() {

        ChatLanguageModel model = VertexAiGeminiChatModel.builder()
                                                         .project(projectId)
                                                         .location(location)
                                                         .modelName("gemini-1.0-pro")
                                                         .build();

        return model.generate(prompt);
    }

    @GetMapping("/langchain/generate/palm")
    public String generate_palm() {

        ChatLanguageModel model = VertexAiChatModel.builder()
                                                   .endpoint(endpoint)
                                                   .publisher("google")
                                                   .project(projectId)
                                                   .location(location)
                                                   .modelName("chat-bison")
                                                   .build();

        Response<AiMessage> response = model.generate(UserMessage.from("用幾句話描述你是什麼語言模型，和用幾句話描述你的代號是什麼，並且使用正體中文回答"));

        return response.content()
                       .text();
    }

    @GetMapping("/langchain/generate/image")
    public ResponseEntity<byte[]> generate_image() {

        VertexAiImageModel model = VertexAiImageModel.builder()
                                                     .endpoint(endpoint)
                                                     .publisher("google")
                                                     .project(projectId)
                                                     .location(location)
                                                     .modelName("imagegeneration")
                                                     .negativePrompt("")
                                                     .maxRetries(2)
//                                                     .seed(19708L)
//                                                     .sampleImageStyle(VertexAiImageModel.ImageStyle.photograph)
                                                     .persistTo(Path.of("tmp/images"))
                                                     .guidanceScale(100)
                                                     .build();

        //        String prompt = "幫我產生一張風景圖片，下面是我的需求「Imagine standing on the top of a hill, overlooking a tranquil lake surrounded by majestic mountains, with a few fluffy clouds floating in the clear blue sky above.」";
        String prompt = "lush forest";

        Response<Image> forestResp = model.generate(prompt);



        String base64Image = forestResp.content()
                                     .base64Data();

        // 解碼 Base64 字串到 byte 陣列
        byte[] imageBytes = Base64.getDecoder()
                                  .decode(base64Image);

        // 創建 HttpHeaders 物件並設置内容類型為透明圖片
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);

        // 返回 ResponseEntity 物件
        return ResponseEntity.ok()
                             .headers(headers)
                             .body(imageBytes);
    }
}
