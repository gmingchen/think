package com.think.modules.review.service;

import com.think.common.pojo.PageResult;
import com.think.core.mybatisplus.service.IServiceX;
import com.think.modules.review.entity.ReviewEntity;
import com.think.modules.review.model.dto.ReviewDTO;
import com.think.modules.review.model.dto.ReviewPageDTO;
import com.think.modules.review.model.dto.ReviewSubmitDTO;

/**
 * 评议
 *
 * @author 好市民
 */
public interface ReviewService extends IServiceX<ReviewEntity> {

    /**
     * 分页查询某决策单下的评议
     * @param dto 分页参数
     * @return 分页结果
     */
    PageResult<ReviewDTO> pageByDecision(ReviewPageDTO dto);

    /**
     * 提交/修改评议（一条有效、禁自评、满员拒绝、截止前可改、公开档走审核）
     * @param dto 参数
     * @return 本人评议
     */
    ReviewDTO submit(ReviewSubmitDTO dto);
}
