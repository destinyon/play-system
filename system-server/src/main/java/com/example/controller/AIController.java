package com.example.controller;

import com.example.common.DataRequest;
import com.example.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

            // 是否注入项目上下文，由前端传入 includeProjectContext 决定（默认不注入）
            boolean includeCtx = false;
            Object includeOpt = payload.get("includeProjectContext");
            if (includeOpt instanceof Boolean) {
                includeCtx = (Boolean) includeOpt;
            } else if (includeOpt instanceof String) {
                includeCtx = Boolean.parseBoolean((String) includeOpt);
            }

            Object originalMessagesObj = payload.get("messages");
            if (includeCtx) {
                List<Map<String, Object>> augmentedMessages = new ArrayList<>();
                String projectContext = readProjectContext(8000);
                if (projectContext != null && !projectContext.isBlank()) {
                    Map<String, Object> contextMsg = new HashMap<>();
                    contextMsg.put("role", "user");
                    contextMsg.put(
                            "content",
                            "[项目上下文]\n" + projectContext +
                                    "\n---\n请在回答问题时优先结合以上背景；若与问题无关，可忽略该上下文。"
                    );
                    augmentedMessages.add(contextMsg);
                }
                if (originalMessagesObj instanceof List) {
                    augmentedMessages.addAll((List<Map<String, Object>>) originalMessagesObj);
                }
                deepseekRequest.put("messages", augmentedMessages);
            } else {
                // 不注入时直接透传
                deepseekRequest.put("messages", originalMessagesObj);
            }
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
                        
                        // DeepSeek Reasoner 模型的思考过程
                        if (message.containsKey("reasoning_content")) {
                            result.put("reasoning_content", message.get("reasoning_content"));
                        }
                        
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

    /**
     * 读取类路径下的项目上下文文件（例如 resources/ai/context.md），并限制最大长度
     */
    private String readProjectContext(int maxLength) {
        try {
            Resource resource = new ClassPathResource("ai/context.md");
            if (!resource.exists()) {
                return null;
            }
            byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (text.length() > maxLength) {
                return text.substring(0, maxLength) + "\n... (已截断)";
            }
            return text;
        } catch (Exception ex) {
            log.warn("读取项目上下文失败: {}", ex.getMessage());
            return null;
        }
    }
}
