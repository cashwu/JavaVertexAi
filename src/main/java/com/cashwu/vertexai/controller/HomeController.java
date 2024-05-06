package com.cashwu.vertexai.controller;

import com.google.cloud.aiplatform.v1.schema.predict.instance.ImageClassificationPredictionInstance;
import com.google.cloud.aiplatform.v1.schema.predict.instance.ImageObjectDetectionPredictionInstance;
import com.google.cloud.aiplatform.v1.schema.predict.instance.ImageSegmentationPredictionInstance;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.*;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;

/**
 * @author cash.wu
 * @since 2024/05/02
 */
@RestController
public class HomeController {

    private final String projectId = System.getenv("PROJECT_ID").trim();
    private final String location = System.getenv("LOCATION").trim();
    private final String modelName = "gemini-1.0-pro";

    private final String prompt = "隨便跟我說一個笑話，使用正體中文回答";
    private final String paLmModel = "text-bison";
    private final String imageModel = "imagegeneration";

    private final String publisher = "google";
    private final String parameters = """
            {
              "temperature": 0.2,
              "maxOutputTokens": 256,
              "topP": 0.95,
              "topK": 40
            }""";

    private final String imageParameters = """
            {
            "sampleCount" : 1
            }""";

    public HomeController() {
    }

    @GetMapping("/")
    public String Index() {

//        System.out.println("project id : " + System.getenv("PROJECT_ID"));
//        System.out.println("location : " + System.getenv("LOCATION"));
        return System.getenv("PROJECT_ID") + " : " + System.getenv("LOCATION");
    }

    @GetMapping("/ai/generate")
    public String generate() throws IOException {

        try (VertexAI vertexAI = new VertexAI(projectId, location)) {

            var model = new GenerativeModel(modelName, vertexAI);

            var generateContentResponse = model.generateContent(prompt);

            String messsage = ResponseHandler.getText(generateContentResponse);

            //            var chatSession = new ChatSession(model);
            //            var response = chatSession.sendMessage(prompt);
            //            ResponseHandler.getText(response);

            return messsage;
        }
    }

    @GetMapping("/ai/generateImage")
    public ResponseEntity<byte[]> generateImage() throws IOException {

        String endpoint = String.format("%s-aiplatform.googleapis.com:443", location);
        var predictionServiceSettings = PredictionServiceSettings.newBuilder()
                                                                 .setEndpoint(endpoint)
                                                                 .build();

        // Initialize client that will be used to send requests. This client only needs to be created
        // once, and can be reused for multiple requests.
        try (PredictionServiceClient predictionServiceClient = PredictionServiceClient.create(predictionServiceSettings)) {

            final EndpointName endpointName = EndpointName.ofProjectLocationPublisherModelName(projectId,
                                                                                               location,
                                                                                               publisher,
                                                                                               imageModel);

            System.out.println("endpoint : " + endpointName);

            //ImageClassificationPredictionInstance
            //ImageSegmentationPredictionInstance
            //ImageObjectDetectionPredictionInstance

            Value.Builder instanceValue = Value.newBuilder();

            String jsonPrompt = """
                    { "prompt": "幫我產生一張風景圖片，下面是我的需求「Imagine standing on the top of a hill, overlooking a tranquil lake surrounded by majestic mountains, with a few fluffy clouds floating in the clear blue sky above.」"},
                    """;

            JsonFormat.parser()
                      .merge(jsonPrompt, instanceValue);
            List<Value> instances = new ArrayList<>();
            instances.add(instanceValue.build());

            Value.Builder parameterValueBuilder = Value.newBuilder();
            JsonFormat.parser()
                      .merge(imageParameters, parameterValueBuilder);
            Value parameterValue = parameterValueBuilder.build();

            // LlmUtilityServiceClient
            // EndpointServiceClient

            PredictResponse predictResponse = predictionServiceClient.predict(endpointName, instances, parameterValue);

            int predictionsCount = predictResponse.getPredictionsCount();

            System.out.println("predictionsCount : " + predictionsCount);

            Value predictions = predictResponse.getPredictions(0);

            Struct structValue = predictions.getStructValue();

            Value.Builder defaultValueBuilder = Value.newBuilder();
            defaultValueBuilder.setStringValue("not value");
            Value defalutValue = defaultValueBuilder.build();
            Value contentValue = structValue.getFieldsOrDefault("bytesBase64Encoded", defalutValue);

            String message = contentValue.getStringValue();

            // 解碼 Base64 字串到 byte 陣列
            byte[] imageBytes = Base64.getDecoder().decode(message.trim());

            // 創建 HttpHeaders 物件並設置内容類型為透明圖片
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG); // 這裡假設圖片為 PNG 格式

            // 返回 ResponseEntity 物件
            return ResponseEntity.ok().headers(headers).body(imageBytes);
        }
    }

    @GetMapping("/ai/generate_palm")
    public String generatePaLM() throws IOException {

        String endpoint = String.format("%s-aiplatform.googleapis.com:443", location);
        var predictionServiceSettings = PredictionServiceSettings.newBuilder()
                                                                 .setEndpoint(endpoint)
                                                                 .build();

        // Initialize client that will be used to send requests. This client only needs to be created
        // once, and can be reused for multiple requests.
        try (PredictionServiceClient predictionServiceClient = PredictionServiceClient.create(predictionServiceSettings)) {

            final EndpointName endpointName = EndpointName.ofProjectLocationPublisherModelName(projectId,
                                                                                               location,
                                                                                               publisher,
                                                                                               paLmModel);

            Value.Builder instanceValue = Value.newBuilder();

            String jsonPrompt = """
                    { "prompt": "給我十個關於專案經理角色的面試問題, 使用正體中文回答"}
                    """;

            JsonFormat.parser()
                      .merge(jsonPrompt, instanceValue);
            List<Value> instances = new ArrayList<>();
            instances.add(instanceValue.build());

            Value.Builder parameterValueBuilder = Value.newBuilder();
            JsonFormat.parser()
                      .merge(parameters, parameterValueBuilder);
            Value parameterValue = parameterValueBuilder.build();

            PredictResponse predictResponse = predictionServiceClient.predict(endpointName, instances, parameterValue);

            Value predictions = predictResponse.getPredictions(0);

            Struct structValue = predictions.getStructValue();

            Value.Builder defaultValueBuilder = Value.newBuilder();
            defaultValueBuilder.setStringValue("not value");
            Value defalutValue = defaultValueBuilder.build();
            Value contentValue = structValue.getFieldsOrDefault("content", defalutValue);

            String message = contentValue.getStringValue();

            return message;
        }
    }
}

