package com.think.modules.decision.model.vo;

import com.think.modules.user.model.vo.UserBriefVO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 评议展示项（先评后看：提交本人评议后可见他人观点与倾向）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class ReviewItemVO {
    /**
     * 评议ID
     */
    private Long id;
    /**
     * 评议人用户ID
     */
    private Long userId;
    /**
     * 倾向备选项下标（0-based）
     */
    private Integer option;
    /**
     * 评议观点文字
     */
    private String content;
    /**
     * 审核闸门状态
     */
    private Integer checkStatus;
    /**
     * 评议时间
     */
    private LocalDateTime createdAt;
    /**
     * 评议人简要信息
     */
    private UserBriefVO user;
}
