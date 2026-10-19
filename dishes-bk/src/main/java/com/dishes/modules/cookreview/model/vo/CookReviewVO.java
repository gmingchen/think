package com.dishes.modules.cookreview.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 烹饪评价列表VO
 * @author 好市民
 */
@Data
public class CookReviewVO {
    /**
     * 评价ID
     */
    private Long id;
    /**
     * 评价人用户ID
     */
    private Long userId;
    /**
     * 评价人昵称
     */
    private String nickname;
    /**
     * 评价人头像
     */
    private String avatar;
    /**
     * 评分（1-5）
     */
    private Integer rating;
    /**
     * 文字评价
     */
    private String content;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
