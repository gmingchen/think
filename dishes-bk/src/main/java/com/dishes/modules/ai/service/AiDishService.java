package com.dishes.modules.ai.service;

import com.dishes.modules.ai.model.dto.AiAdoptDTO;
import com.dishes.modules.ai.model.dto.AiImageGenDTO;
import com.dishes.modules.ai.model.dto.AiParseCommitDTO;
import com.dishes.modules.ai.model.dto.AiParseDTO;
import com.dishes.modules.ai.model.dto.AiRecommendDTO;
import com.dishes.modules.ai.model.vo.AiImageVO;
import com.dishes.modules.ai.model.vo.AiParseVO;
import com.dishes.modules.ai.model.vo.AiRecommendVO;

/**
 * AI 荐菜
 * @author 好市民
 */
public interface AiDishService {

    /**
     * 生成推荐：规则粗筛 + LLM 排序并生成新菜谱
     * @param dto 请求参数
     * @return 两组推荐结果与采纳凭证
     */
    AiRecommendVO recommend(AiRecommendDTO dto);

    /**
     * 采纳第二组新菜谱，落库为正式菜谱
     * @param dto 请求参数（推荐凭证 + 下标）
     * @return 新菜谱ID
     */
    Long adopt(AiAdoptDTO dto);

    /**
     * 今日剩余荐菜次数（只读 Redis 已用计数，不自增）
     * @return 剩余次数
     */
    Integer remainCount();

    /**
     * 今日剩余文案识别次数（只读 Redis 已用计数，不自增）
     * @return 剩余次数
     */
    Integer parseRemainCount();

    /**
     * 今日剩余 AI 封面生图次数（只读 Redis 已用计数，不自增）
     * @return 剩余次数
     */
    Integer imageRemainCount();

    /**
     * 文案识别预览：LLM 将做法文案解析为结构化数据（仅名字，不建库、不落库）
     * @param dto 请求参数（厨房 + 文案）
     * @return 结构化草稿（无 id，供弹窗预览）
     */
    AiParseVO parse(AiParseDTO dto);

    /**
     * 文案识别确认：将预览的名字解析进食材/调料库、匹配分类，返回带 id 的可回填结构（不写 dish 表）
     * @param dto 请求参数（厨房 + parse 回传结构）
     * @return 带 id 的回填结构
     */
    AiParseVO commit(AiParseCommitDTO dto);

    /**
     * AI 封面生图：拼 prompt 调云函数 image-proxy 得 base64 dataURL，不上传存储、不写 dish 表
     * @param dto 请求参数（厨房 + 当前表单已有菜名/分类/食材/调料）
     * @return base64 dataURL，前端写入临时文件展示与确认上传
     */
    AiImageVO genImage(AiImageGenDTO dto);
}
