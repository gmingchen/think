package com.dishes.modules.ai.model.cache;

import com.dishes.modules.ai.model.vo.AiRecommendVO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * AI 荐菜暂存（Redis，采纳凭证）
 * <p>
 * 仅缓存第二组新菜谱（first 组随推荐即时展示、无需采纳），记录归属用户与厨房用于采纳时校验
 * </p>
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class AiRecommendCache implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 归属用户ID（采纳时校验凭证归属）
     */
    private Long userId;
    /**
     * 归属厨房ID（采纳时落库目标）
     */
    private Long kitchenId;
    /**
     * 第二组新菜谱列表
     */
    private List<AiRecommendVO.FreshDish> fresh;
}
