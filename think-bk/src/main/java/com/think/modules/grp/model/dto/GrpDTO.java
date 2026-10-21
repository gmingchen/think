package com.think.modules.grp.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群组信息
 *
 * @author 好市民
 */
@Data
public class GrpDTO {
    /**
     * 群ID
     */
    private Long id;
    /**
     * 群名称
     */
    private String name;
    /**
     * 群头像
     */
    private String avatar;
    /**
     * 群主用户ID
     */
    private Long userId;
    /**
     * 状态：1-正常 2-已解散
     */
    private Integer status;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
