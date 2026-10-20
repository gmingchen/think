package com.think.modules.ai.client;

import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.think.common.utils.StringUtils;
import com.think.exception.RunException;
import com.think.modules.ai.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 生图客户端
 * <p>
 * 通过 CloudBase 云函数 image-proxy 中转调用混元 HY-Image-3.0（成长计划二期赠送的 10 万张图像额度
 * 仅限小程序 SDK / 云函数消耗，后端不可直连）。请求体 {@code { prompt, size }}，响应体
 * {@code { imageBase64: "data:image/png;base64,..." }}（云函数已统一转 dataURL）。
 * </p>
 * @author 好市民
 */
@Slf4j
@Component
public class ImageClient {

    /**
     * 共享密钥请求头，与云函数侧 process.env.PROXY_KEY 对齐（与 llm-proxy 共用同值）
     */
    private static final String PROXY_KEY_HEADER = "x-proxy-key";

    @Autowired
    private AiProperties aiProperties;

    /**
     * 生成图片：发送 prompt，返回 base64 dataURL
     * @param prompt 生图文本描述（含风格锚与菜谱信息，见 AiImagePrompt.build）
     * @return 形如 {@code data:image/png;base64,...} 的 dataURL
     */
    public String generate(String prompt) {
        AiProperties.Image image = aiProperties.getImage();
        if (StringUtils.isBlank(image.getUrl())) {
            throw new RunException("AI 生图功能未开通");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("prompt", prompt);
        body.put("size", image.getSize());
        // 模型名一并下传：二期赠额对应模型若下线，只改 yml 即可，不必重新部署云函数
        body.put("model", image.getModel());

        String response;
        try (HttpResponse executed = HttpRequest.post(image.getUrl())
                .header(PROXY_KEY_HEADER, image.getProxyKey())
                .contentType(ContentType.JSON.getValue())
                .timeout(image.getTimeout())
                .body(JSONUtil.toJsonStr(body))
                .execute()) {
            int status = executed.getStatus();
            String rawBody = executed.body();
            if (!executed.isOk()) {
                log.error("生图中转请求失败，status: {} body: {}", status, StringUtils.sub(rawBody, 0, 500));
                // 云函数返回的 error 字段透传给用户可读提示（如审核拒 / 并发超限）
                throw new RunException(mapErrorMessage(status, rawBody));
            }
            response = rawBody;
        } catch (RunException e) {
            throw e;
        } catch (Exception e) {
            log.error("生图中转请求异常", e);
            throw new RunException("AI 生图服务异常，请稍后再试");
        }
        return extractImageBase64(response);
    }

    /**
     * 从云函数响应中提取 imageBase64 字段
     */
    private String extractImageBase64(String response) {
        try {
            JSONObject json = JSONUtil.parseObj(response);
            String b64 = json.getStr("imageBase64");
            if (StringUtils.isBlank(b64)) {
                log.error("生图响应缺少 imageBase64 字段，response: {}", StringUtils.sub(response, 0, 500));
                throw new RunException("AI 未返回图像，请稍后重试");
            }
            return b64;
        } catch (RunException e) {
            throw e;
        } catch (Exception e) {
            log.error("生图响应解析失败，response: {}", StringUtils.sub(response, 0, 500), e);
            throw new RunException("AI 生图返回异常，请稍后再试");
        }
    }

    /**
     * 依据 HTTP 状态与 error 字段映射用户可读消息
     */
    private String mapErrorMessage(int status, String rawBody) {
        String err = "";
        try {
            if (StringUtils.isNotBlank(rawBody)) {
                err = JSONUtil.parseObj(rawBody).getStr("error");
            }
        } catch (Exception ignore) {
            // 响应体非 JSON 时忽略
        }
        if (status == 503 || (err != null && err.contains("EXCEED_CONCURRENT_REQUEST_LIMIT"))) {
            return "AI 忙，请稍后重试";
        }
        if (err != null && (err.contains("TextIllegal") || err.contains("GenerateImageFailed"))) {
            return "描述包含敏感内容，请调整菜名/食材后重试";
        }
        return "AI 生图服务异常，请稍后再试";
    }
}
