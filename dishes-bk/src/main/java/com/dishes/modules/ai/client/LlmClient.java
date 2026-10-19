package com.dishes.modules.ai.client;

import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dishes.common.utils.StringUtils;
import com.dishes.exception.RunException;
import com.dishes.modules.ai.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 客户端
 * <p>
 * 通过 CloudBase 云函数 llm-proxy 中转调用混元（成长计划赠送额度仅限小程序 SDK / 云函数消耗，后端不可直连），
 * 请求/响应体均为 OpenAI chat/completions 兼容结构
 * </p>
 * @author 好市民
 */
@Slf4j
@Component
public class LlmClient {

    /**
     * 共享密钥请求头，与云函数侧 process.env.PROXY_KEY 对齐
     */
    private static final String PROXY_KEY_HEADER = "x-proxy-key";

    @Autowired
    private AiProperties aiProperties;

    /**
     * 会话补全：发送 messages，返回模型输出的纯文本
     * @param messages 消息列表，元素形如 {role: system|user, content: ...}
     * @param temperature 生成温度，null 时用配置默认值
     * @return 模型输出文本
     */
    public String chat(List<Map<String, Object>> messages, Double temperature) {
        AiProperties.Llm llm = aiProperties.getLlm();
        if (StringUtils.isBlank(llm.getUrl())) {
            throw new RunException("AI 功能未开通");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", llm.getModel());
        body.put("messages", messages);
        body.put("temperature", temperature != null ? temperature : llm.getTemperature());
        body.put("stream", false);

        String response;
        try (HttpResponse executed = HttpRequest.post(llm.getUrl())
                .header(PROXY_KEY_HEADER, llm.getProxyKey())
                .contentType(ContentType.JSON.getValue())
                .timeout(llm.getTimeout())
                .body(JSONUtil.toJsonStr(body))
                .execute()) {
            if (!executed.isOk()) {
                log.error("LLM 中转请求失败，status: {}", executed.getStatus());
                throw new RunException("AI 服务异常，请稍后再试");
            }
            response = executed.body();
        } catch (RunException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM 中转请求异常", e);
            throw new RunException("AI 服务异常，请稍后再试");
        }
        return extractContent(response);
    }

    /**
     * 便捷构造一条消息
     * @param role system / user
     * @param content 内容
     * @return 消息 Map
     */
    public static Map<String, Object> message(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    /**
     * 从 OpenAI 兼容响应中提取 choices[0].message.content
     */
    private String extractContent(String response) {
        try {
            JSONObject json = JSONUtil.parseObj(response);
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RunException("AI 未返回结果");
            }
            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            String content = message == null ? null : message.getStr("content");
            // 兼容云函数直接返回 text 字段的降级结构
            if (StringUtils.isBlank(content)) {
                content = choice.getStr("text");
            }
            if (StringUtils.isBlank(content)) {
                throw new RunException("AI 未返回结果");
            }
            return content;
        } catch (RunException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM 响应解析失败，response: {}", StringUtils.sub(response, 0, 500), e);
            throw new RunException("AI 返回异常，请稍后再试");
        }
    }
}
