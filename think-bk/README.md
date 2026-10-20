# 厨菜记后端（think-bk）

厨菜记微信小程序后端服务。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 1.8 | JDK |
| Spring Boot | 2.7.9 | Web 框架 |
| Spring Security | - | 安全认证框架 |
| MyBatis-Plus | 3.5.3.1 | ORM 持久层 |
| MySQL | 8.0 | 数据库 |
| Redis | - | 缓存 |
| Druid | 1.2.9 | 数据库连接池 |
| JWT | 0.9.1 | Token 认证 |
| MapStruct | 1.5.3.Final | 对象转换 |
| Lombok | 1.18.24 | 简化代码 |
| Hutool | 5.8.15 | 工具库 |

## 项目结构

```
think-bk/
├── src/main/java/com/dishes/
│   ├── core/           # 核心模块（安全、配置等）
│   ├── module/         # 业务模块
│   └── common/         # 公共模块（工具类、枚举、异常等）
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-test.yml
│   └── application-prod.yml
└── pom.xml
```

## 环境要求

- JDK 1.8
- MySQL >= 8.0
- Redis

## 快速开始

1. 克隆项目
2. 配置 `application-dev.yml` 中的数据库和 Redis 连接信息
3. 运行 `ThinkApplication` 启动类
