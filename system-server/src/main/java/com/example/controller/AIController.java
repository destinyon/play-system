package com.example.controller;

import com.example.common.DataRequest;
import com.example.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    @Value("${deepseek.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * AI聊天接口 - 代理请求到DeepSeek API
     * 
     * @param request DataRequest包含data.messages数组
     * @return AI响应
     */
    @PostMapping("/chat")
    public Result chat(@RequestBody DataRequest request) {
        try {
            // 验证请求参数
            if (request.getData() == null || !request.getData().containsKey("messages")) {
                return Result.error("缺少messages参数");
            }

            // 构建DeepSeek API请求体（允许前端覆盖模型与stream配置）
            Map<String, Object> payload = request.getData();
            Object modelOverride = payload.get("model");
            String resolvedModel = model;
            if (modelOverride instanceof String && !((String) modelOverride).isBlank()) {
                resolvedModel = (String) modelOverride;
            }

            Object streamOpt = payload.get("stream");
            boolean streamFlag = streamOpt instanceof Boolean ? (Boolean) streamOpt : false;

            Map<String, Object> deepseekRequest = new HashMap<>();
            deepseekRequest.put("model", resolvedModel);
            // 直接透传messages，支持富内容（如图片/文件）结构
            deepseekRequest.put("messages", payload.get("messages"));
            deepseekRequest.put("stream", streamFlag);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(deepseekRequest, headers);

            // 调用DeepSeek API
            String endpoint = apiUrl + "/chat/completions";
            log.info("Calling DeepSeek API: {}", endpoint);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // 提取AI回复内容
                if (responseBody.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    if (!choices.isEmpty()) {
                        Map<String, Object> firstChoice = choices.get(0);
                        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                        
                        Map<String, Object> result = new HashMap<>();
                        result.put("content", message.get("content"));
                        result.put("role", message.get("role"));
                        
                        // 可选：包含token使用信息
                        if (responseBody.containsKey("usage")) {
                            result.put("usage", responseBody.get("usage"));
                        }
                        
                        return Result.success(result);
                    }
                }
                
                // 如果响应格式不符合预期，返回原始响应
                return Result.success(responseBody);
            } else {
                log.error("DeepSeek API返回非200状态码: {}", response.getStatusCode());
                return Result.error("AI服务响应异常");
            }

        } catch (Exception e) {
            log.error("调用DeepSeek API失败", e);
            return Result.error("调用AI服务失败: " + e.getMessage());
        }
    }
}
