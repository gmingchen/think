package com.think.modules.ai.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * AI 封面生图结果VO
 * <p>
 * 承载云函数 image-proxy 返回的 base64 dataURL（形如 {@code data:image/png;base64,...}）；
 * 前端拿到后写入临时文件展示与预览，用户确认「使用这张」时才走现有 uploadMedia 上传
 * </p>
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class AiImageVO {
    /**
     * 生成图片的 base64 dataURL（含 mime 前缀，右下角带混元默认「图片由 AI 生成」合规水印）
     */
    private String imageBase64;
}
