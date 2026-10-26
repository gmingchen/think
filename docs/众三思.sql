-- =============================================================
-- 众三思 · 数据库建表脚本（MVP）
-- 依据：docs/数据库设计.md（需求冻结版）
-- 数据库：MySQL 8.0 / InnoDB / utf8mb4 / utf8mb4_0900_ai_ci
-- 约定：
--   1. 全表统一基础字段 id / created_at / updated_at / deleted_at / deleted
--   2. 外键为逻辑外键（不建物理 FOREIGN KEY），配合普通索引
--   3. 关联表（多对多中间表）用双下划线连接两表名，如 decision__tag
--   4. 软删除：deleted（0-未删除 1-已删除）+ deleted_at（删除时间）
--   5. 枚举字段用 TINYINT 存储，取值含义见 COMMENT
--
-- 维护约定：本文件是数据库 schema 的权威来源，随版本迭代始终保持最新最全；
--          任何表结构变更（新增表/增删字段/调整索引/修改种子数据）必须同步更新本文件，
--          保证其随时可独立执行、一次性完成建库。
--          ⚠️ 本脚本是全量重建脚本（先 DROP 再 CREATE），只用于本地/新环境初始化，
--             切勿对带真实数据的库执行；线上表结构变更只单独跑 ALTER 语句。
-- =============================================================

-- -------------------------------------------------------------
-- 0. 删除表（置于顶部，令本脚本可重复执行：先清空旧表，再重建并灌种子数据）
--    全表为逻辑外键、无物理 FOREIGN KEY，故删除/创建顺序不受约束限制；
--    此处按「依赖倒序」排列（先删引用方，后删被引用方），仅为可读性。
--    ⚠️ 新增表时必须同步补进本清单：media_check 曾因漏在此处登记，
--       导致第二次执行卡在「table already exists」，现清单与建表段严格一一对应（共 18 张）。
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `operation_log`;
DROP TABLE IF EXISTS `media_check`;
DROP TABLE IF EXISTS `review_check`;
DROP TABLE IF EXISTS `decision_check`;
DROP TABLE IF EXISTS `abuse_report`;
DROP TABLE IF EXISTS `subscribe_log`;
DROP TABLE IF EXISTS `subscribe`;
DROP TABLE IF EXISTS `subscribe_template`;
DROP TABLE IF EXISTS `recap`;
DROP TABLE IF EXISTS `report`;
DROP TABLE IF EXISTS `review`;
DROP TABLE IF EXISTS `decision__tag`;
DROP TABLE IF EXISTS `tag`;
DROP TABLE IF EXISTS `decision`;
DROP TABLE IF EXISTS `grp_member`;
DROP TABLE IF EXISTS `grp`;
DROP TABLE IF EXISTS `user_login_log`;
DROP TABLE IF EXISTS `user`;

-- -------------------------------------------------------------
-- 1. user 用户表（§3.1 openid 身份）
-- -------------------------------------------------------------
CREATE TABLE `user` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `openid`       VARCHAR(64)  NOT NULL COMMENT '微信openid',
  `unionid`      VARCHAR(64)  DEFAULT NULL COMMENT '微信unionid（预留）',
  `nickname`     VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
  `avatar`       VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像（预设标识）',
  `sex`          TINYINT      NOT NULL DEFAULT 2 COMMENT '性别：0-女 1-男 2-未知',
  `inviter_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '邀请人用户ID（群邀请拉新溯源）',
  `invite_count` INT          NOT NULL DEFAULT 0 COMMENT '邀请用户数（冗余计数）',
  `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at`   DATETIME     DEFAULT NULL COMMENT '删除时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_inviter` (`inviter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- -------------------------------------------------------------
-- 2. user_login_log 用户登录流水表（append-only，DAU 统计源）
-- -------------------------------------------------------------
CREATE TABLE `user_login_log` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '登录日志ID',
  `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '登录用户ID',
  `openid`     VARCHAR(64) NOT NULL COMMENT '微信openid',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME    DEFAULT NULL COMMENT '删除时间',
  `deleted`    TINYINT     NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户登录流水表（只增不改，DAU 统计源）';

-- -------------------------------------------------------------
-- 3. grp 群组表（§3.1 群只是分组概念）
-- -------------------------------------------------------------
CREATE TABLE `grp` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '群ID',
  `name`       VARCHAR(64) NOT NULL COMMENT '群名称',
  `avatar`     VARCHAR(255) NOT NULL DEFAULT '' COMMENT '群头像（预设标识，群主可修改）',
  `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '群主用户ID',
  `status`     TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常 2-已解散',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  `deleted`    TINYINT  NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='群组表';

-- -------------------------------------------------------------
-- 4. grp_member 群成员表（群内可见性判定依据）
-- -------------------------------------------------------------
CREATE TABLE `grp_member` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `grp_id`     BIGINT UNSIGNED NOT NULL COMMENT '所属群ID',
  `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '成员用户ID',
  `role`       TINYINT NOT NULL DEFAULT 2 COMMENT '角色：1-群主 2-成员',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间（退群/群解散时写入）',
  `deleted`    TINYINT  NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_grp_user` (`grp_id`, `user_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='群成员表';

-- -------------------------------------------------------------
-- 5. decision 决策单表（§3.4 生命周期核心主表）
-- -------------------------------------------------------------
CREATE TABLE `decision` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '决策单ID',
  `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '发起人用户ID',
  `title`            VARCHAR(120) NOT NULL COMMENT '决策单标题（<=30字，列表与详情展示）',
  `content`          TEXT NOT NULL COMMENT '决策内容（自由描述两难处境、背景与纠结点）',
  `options`          VARCHAR(2000) NOT NULL COMMENT '备选方案文案数组，>=2项，JSON 字符串数组（如 ["选项一","选项二"]）；发布后锁定，review.option 按下标对齐',
  `images`           VARCHAR(1000) DEFAULT NULL COMMENT '决策单图片URL数组，JSON 字符串数组，最多3张（可选）',
  `visibility`       TINYINT NOT NULL DEFAULT 1 COMMENT '可见范围：1-仅自己(草稿) 2-群内 3-公开',
  `grp_id`           BIGINT UNSIGNED DEFAULT NULL COMMENT '目标群ID（visibility=2 必填，发起人须为该群成员，不限于群主）',
  `cool_down_minutes` INT UNSIGNED NOT NULL DEFAULT 1440 COMMENT '评议时限(分钟)：1~1440（≤24小时）；0=不限（靠人数达标或手动结束收口）',
  `cool_down_end_at` DATETIME DEFAULT NULL COMMENT '评议时限截止时间 = 发布时间 + 时长（不限时则为 NULL）',
  `review_limit`     INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '评议人数上限：>=1；群内<=群成员数；公开<=100',
  `status`           TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-草稿 1-评议中 2-已截止 3-已复盘 4-已关闭',
  `check_status`     TINYINT NOT NULL DEFAULT 0 COMMENT '文本审核闸门（公开/群内档均适用，仅自己恒为0）：0-无需审核 1-待审核 2-通过 3-不通过；通过才激活（公开进广场、群内进评议中）；图片另走异步 media_check 不回填本列', 
  `ended_early`      TINYINT NOT NULL DEFAULT 0 COMMENT '未满评议时限标记：0-否 1-提前结束',
  `report_generated` TINYINT NOT NULL DEFAULT 0 COMMENT '报告是否已生成：0-否 1-是',
  `published_at`     DATETIME DEFAULT NULL COMMENT '发布时间（进入评议中时间）',
  `created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at`       DATETIME DEFAULT NULL COMMENT '删除时间',
  `deleted`          TINYINT  NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_grp` (`grp_id`),
  KEY `idx_visibility_status` (`visibility`, `status`),
  KEY `idx_cooldown_end` (`status`, `cool_down_end_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='决策单表';

-- -------------------------------------------------------------
-- 6. tag 标签表（预置主数据，圈子雏形）
-- -------------------------------------------------------------
CREATE TABLE `tag` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name`       VARCHAR(30) NOT NULL COMMENT '标签名称',
  `sort`       INT NOT NULL DEFAULT 0 COMMENT '展示排序',
  `status`     TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用（禁用后不再可选，历史关联保留）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  `deleted`    TINYINT  NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签表（预置主数据，圈子雏形）';

-- -------------------------------------------------------------
-- 7. decision__tag 决策单标签关联表（多对多，仅公开档，<=3）
-- -------------------------------------------------------------
CREATE TABLE `decision__tag` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `decision_id` BIGINT UNSIGNED NOT NULL COMMENT '决策单ID',
  `tag_id`      BIGINT UNSIGNED NOT NULL COMMENT '标签ID',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at`  DATETIME DEFAULT NULL COMMENT '删除时间',
  `deleted`     TINYINT  NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_decision__tag` (`decision_id`, `tag_id`),
  KEY `idx_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='决策单标签关联表';

-- -------------------------------------------------------------
-- 8. review 评议表（一条有效、禁自评、先评后看）
-- -------------------------------------------------------------
CREATE TABLE `review` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评议ID',
  `decision_id`  BIGINT UNSIGNED NOT NULL COMMENT '所属决策单ID',
  `user_id`      BIGINT UNSIGNED NOT NULL COMMENT '评议人用户ID（!= 发起人，禁自评）',
  `option`       INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '倾向备选项下标（0-based，< options 数组长度）；MySQL 保留字，SQL 中须加反引号',
  `content`      VARCHAR(500) NOT NULL COMMENT '评议观点文字（不能为空，应用层校验）',
  `check_status` TINYINT NOT NULL DEFAULT 0 COMMENT '评议审核闸门（公开/群内档 decision 下的评议适用）：0-无需 1-待审 2-通过 3-不通过；命中违规采用拒绝提交（本次落库整体回滚、不占名额），故已提交评议本列实际恒为2',
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（修改评议走 UPDATE）',
  `deleted_at`   DATETIME DEFAULT NULL COMMENT '删除时间',
  `deleted`      TINYINT  NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_decision_user` (`decision_id`, `user_id`),
  KEY `idx_decision_created` (`decision_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评议表';

-- -------------------------------------------------------------
-- 9. report 决策报告表（截止/提前结束时生成；仅发起人可见）
-- -------------------------------------------------------------
CREATE TABLE `report` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '报告ID',
  `decision_id`    BIGINT UNSIGNED NOT NULL COMMENT '决策单ID',
  `option_counts`  JSON NOT NULL COMMENT '各备选项有效人数，如 {"0":3,"1":5,"2":1}（下标->计数）',
  `total_review`   INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '有效评议总数',
  `is_ai_fallback` TINYINT NOT NULL DEFAULT 0 COMMENT '零评议AI兜底：0-否 1-是',
  `ai_content`     TEXT DEFAULT NULL COMMENT 'AI三思参考正文（仅兜底时存）',
  `generated_at`   DATETIME NOT NULL COMMENT '报告生成时间',
  `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at`     DATETIME DEFAULT NULL COMMENT '删除时间',
  `deleted`        TINYINT  NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_decision` (`decision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='决策报告表';

-- -------------------------------------------------------------
-- 10. recap 决策复盘表（发起人回填结局，可选）
-- -------------------------------------------------------------
CREATE TABLE `recap` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '复盘ID',
  `decision_id`   BIGINT UNSIGNED NOT NULL COMMENT '决策单ID',
  `final_choice`  INT NOT NULL DEFAULT -1 COMMENT '最终选择：options 下标（0-based）；-1=其他/未选',
  `actual_result` TEXT DEFAULT NULL COMMENT '实际发生了什么',
  `reflected_at`  DATETIME DEFAULT NULL COMMENT '回填时间',
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at`    DATETIME DEFAULT NULL COMMENT '删除时间',
  `deleted`       TINYINT  NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_decision` (`decision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='决策复盘表';

-- -------------------------------------------------------------
-- 11. subscribe_template 订阅消息模板配置表
-- -------------------------------------------------------------
CREATE TABLE `subscribe_template` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `type`        VARCHAR(30) NOT NULL COMMENT '通知类型：COOL_DOWN_END-评议时限到期提醒（预留扩展）',
  `template_id` VARCHAR(50) NOT NULL COMMENT '微信订阅消息模板ID',
  `remark`      VARCHAR(100) DEFAULT NULL COMMENT '备注',
  `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at`  DATETIME    DEFAULT NULL COMMENT '删除时间',
  `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订阅消息模板配置表';

-- -------------------------------------------------------------
-- 12. subscribe 用户订阅额度记录表（一次性订阅：授权 +1，发送 -1）
-- -------------------------------------------------------------
CREATE TABLE `subscribe` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '订阅用户ID',
  `template_id`     VARCHAR(50) NOT NULL COMMENT '微信模板ID',
  `remaining_count` INT NOT NULL DEFAULT 0 COMMENT '剩余可发次数',
  `created_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at`      DATETIME    DEFAULT NULL COMMENT '删除时间',
  `deleted`         TINYINT     NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_template` (`user_id`, `template_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户订阅额度记录表';

-- -------------------------------------------------------------
-- 13. subscribe_log 订阅消息发送日志表
-- -------------------------------------------------------------
CREATE TABLE `subscribe_log` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '接收人用户ID',
  `template_id` VARCHAR(50) NOT NULL COMMENT '微信模板ID',
  `type`        VARCHAR(30) NOT NULL COMMENT '通知类型（COOL_DOWN_END）',
  `biz_type`    VARCHAR(30) NOT NULL DEFAULT '' COMMENT '业务对象类型（本项目：DECISION-决策单）',
  `biz_id`      BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '业务对象ID（本项目=decision.id）',
  `status`      TINYINT NOT NULL COMMENT '发送状态：1-成功 2-失败',
  `message`     VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at`  DATETIME    DEFAULT NULL COMMENT '删除时间',
  `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订阅消息发送日志表';

-- -------------------------------------------------------------
-- 14. abuse_report 举报表（仅公开档；MVP 无运营后台，落库留痕、延后处置）
-- -------------------------------------------------------------
CREATE TABLE `abuse_report` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '举报ID',
  `target_type`   TINYINT NOT NULL DEFAULT 1 COMMENT '举报对象类型：1-决策单 2-评议',
  `target_id`     BIGINT UNSIGNED NOT NULL COMMENT '对象ID',
  `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '举报人用户ID',
  `reason`        VARCHAR(255) NOT NULL COMMENT '举报理由',
  `handle_status` TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0-待处理 1-已处理(下架) 2-驳回',
  `handle_note`   VARCHAR(255) DEFAULT '' COMMENT '处理备注',
  `handled_at`    DATETIME DEFAULT NULL COMMENT '处理时间',
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at`    DATETIME DEFAULT NULL COMMENT '删除时间',
  `deleted`       TINYINT  NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_status` (`handle_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='举报表（仅公开档）';

-- -------------------------------------------------------------
-- 15. decision_check 决策单文本审核流水表（公开/群内档；每次审核一行）
-- -------------------------------------------------------------
CREATE TABLE `decision_check` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `decision_id`  BIGINT UNSIGNED NOT NULL COMMENT '被审决策单ID',
  `provider`     VARCHAR(16) NOT NULL DEFAULT 'wx' COMMENT '审核渠道',
  `suggest`      VARCHAR(16) DEFAULT '' COMMENT '微信返回：pass/review/risky',
  `check_status` TINYINT NOT NULL DEFAULT 0 COMMENT '单次审核结果：0-待审 1-通过 2-不通过 3-需人工复审（与 decision.check_status 闸门态语义不同）',
  `detail`       VARCHAR(500) DEFAULT '' COMMENT '命中标签/明细',
  `checked_at`   DATETIME DEFAULT NULL COMMENT '审核时间',
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at`   DATETIME DEFAULT NULL COMMENT '删除时间',
  `deleted`      TINYINT  NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_decision` (`decision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='决策单文本审核流水表（公开/群内档）';

-- -------------------------------------------------------------
-- 16. review_check 评议文本审核流水表（公开/群内档；每次审核一行）
-- -------------------------------------------------------------
CREATE TABLE `review_check` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `review_id`    BIGINT UNSIGNED NOT NULL COMMENT '被审议ID',
  `provider`     VARCHAR(16) NOT NULL DEFAULT 'wx' COMMENT '审核渠道',
  `suggest`      VARCHAR(16) DEFAULT '' COMMENT '微信返回：pass/review/risky',
  `check_status` TINYINT NOT NULL DEFAULT 0 COMMENT '单次审核结果：0-待审 1-通过 2-不通过 3-需人工复审（与 review.check_status 闸门态语义不同）',
  `detail`       VARCHAR(500) DEFAULT '' COMMENT '命中标签/明细',
  `checked_at`   DATETIME DEFAULT NULL COMMENT '审核时间',
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at`   DATETIME DEFAULT NULL COMMENT '删除时间',
  `deleted`      TINYINT  NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_review` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评议文本审核流水表（公开/群内档）';

-- -------------------------------------------------------------
-- 17. media_check 多媒体内容审核流水表（图片/头像异步送审；每次送审一行）
-- -------------------------------------------------------------
CREATE TABLE `media_check` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `biz_type`     TINYINT NOT NULL COMMENT '资产类型：1-决策单图片 2-用户头像',
  `biz_id`       BIGINT UNSIGNED NOT NULL COMMENT '业务主键：决策单ID 或 用户ID',
  `target`       INT DEFAULT NULL COMMENT '图片在 images 数组中的下标（头像为空）',
  `media_url`    VARCHAR(512) NOT NULL COMMENT '送审的图片URL（回调命中违规时按此URL精确移除，防下标漂移误删）',
  `trace_id`     VARCHAR(64) DEFAULT NULL COMMENT '微信 media_checkAsync 返回的 trace_id（异步结果关联键）',
  `openid`       VARCHAR(64) DEFAULT NULL COMMENT '内容发布者 openid',
  `scene`        TINYINT DEFAULT NULL COMMENT '场景值 1-资料 2-评论 3-论坛 4-社交日志',
  `provider`     VARCHAR(16) NOT NULL DEFAULT 'wx' COMMENT '审核渠道',
  `suggest`      VARCHAR(16) DEFAULT 'pending' COMMENT '微信回调结论：pass/review/risky（未回调为 pending）',
  `check_status` TINYINT NOT NULL DEFAULT 0 COMMENT '单次审核结果：0-待审 1-通过 2-不通过 3-需人工复审',
  `detail`       VARCHAR(500) DEFAULT '' COMMENT '命中标签/明细',
  `checked_at`   DATETIME DEFAULT NULL COMMENT '审核时间（回调落库时间）',
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at`   DATETIME DEFAULT NULL COMMENT '删除时间',
  `deleted`      TINYINT  NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trace` (`trace_id`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多媒体内容审核流水表（图片/头像异步送审）';

-- -------------------------------------------------------------
-- 18. operation_log 操作审计日志表（append-only，不随业务级联删除）
-- -------------------------------------------------------------
CREATE TABLE `operation_log` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人用户ID（运营/管理员后台操作为 NULL）',
  `action`     VARCHAR(50) NOT NULL COMMENT '操作类型：REPORT_HANDLE/GRP_DISSOLVE/DECISION_CLOSE/DECISION_DELETE/CHECK_HANDLE 等',
  `biz_type`   VARCHAR(30) NOT NULL DEFAULT '' COMMENT '业务对象类型：DECISION/REVIEW/GRP/ABUSE_REPORT/TAG（可扩展）',
  `biz_id`     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '业务对象ID（无对象关联时置 0）',
  `detail`     VARCHAR(500) DEFAULT '' COMMENT '操作详情（如举报处理结果备注）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  `deleted`    TINYINT  NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作审计日志表（append-only，不随业务删除）';

-- =============================================================
-- 种子数据
-- =============================================================

-- 预置标签（MVP 无运营后台，随建表脚本预置；运营维护延后二期）
INSERT INTO `tag` (`name`, `sort`, `status`) VALUES
('职场', 1, 1),
('感情', 2, 1),
('婚姻', 3, 1),
('消费', 4, 1),
('理财', 5, 1),
('房产', 6, 1),
('学业', 7, 1),
('健康', 8, 1),
('家庭', 9, 1),
('生活', 10, 1);

-- 订阅消息模板配置（微信后台「投票结束通知」模板）：
-- ⚠️ template_id 必须与小程序端常量 DECISION_REMIND_TMPL_ID
--    （think-mp/common/constants/decision/index.js）同值：
--    前端授权用的 ID 经 /subscribe/sync 记进 subscribe 表，后端再按本行的 ID 扣配额发送，
--    两处不一致 → consume 扣不到 → 到期提醒静默不发
-- 关键词顺序固定：投票标题→thing1、结束时间→time2、温馨提示→thing3（详见 CoolDownEndTask）
INSERT INTO `subscribe_template` (`type`, `template_id`, `remark`) VALUES
('COOL_DOWN_END', 'wZjDvP-cW6DukURxbF7YZ0nOuf-ht95kLaMZz8YmNLM', '评议时限到期提醒');
