# fu-ai-code

基于 LangChain4j + LangGraph4j 的 AI 代码生成平台，支持 HTML 页面、多文件代码、Vue 项目等多种类型的智能生成。

## 项目简介

fu-ai-code 是一个全栈 AI 代码生成平台，采用工作流编排的方式，将图片收集、Prompt 增强、智能路由、代码生成、质量检查、项目构建等多个环节串联起来，实现从自然语言描述到可运行代码的端到端生成。

## 技术栈

### 后端

- **框架**: Spring Boot 3.5.14 + Java 21
- **ORM**: MyBatis-Flex 1.11.0
- **数据库**: MySQL 8.0 + HikariCP 连接池
- **缓存**: Redis + Spring Session
- **AI 框架**: LangChain4j 1.1.0 + LangGraph4j 1.6.0
- **分布式限流/锁**: Redisson 3.50.0
- **接口文档**: Knife4j 4.4.0 (OpenAPI 3)
- **工具库**: Hutool 5.8.38 + Lombok
- **网页截图**: Selenium 4.33.0
- **对象存储**: 腾讯云 COS

### 前端

- **框架**: Vue 3.4 + Vite 4.5
- **UI 组件库**: Element Plus 2.7
- **状态管理**: Pinia 2.1
- **路由**: Vue Router 4.3
- **HTTP 客户端**: Axios 1.6
- **日期处理**: Day.js 1.11

## 核心功能

### 1. 多类型代码生成

- **HTML 页面生成**: 生成单页 HTML 代码，包含样式和交互
- **多文件代码生成**: 生成包含多个文件的代码项目
- **Vue 项目生成**: 生成完整的 Vue 3 项目结构，支持自动构建

### 2. 智能工作流（LangGraph4j）

基于 LangGraph4j 编排的多节点工作流：

```
图片收集 → Prompt 增强 → 智能路由 → 代码生成 → 质量检查 → 项目构建
                                                                 ↓
                                                               完成
```

- **ImageCollectorNode**: 智能收集配图资源（Logo、插图、图片搜索）
- **PromptEnhancerNode**: 优化用户 Prompt，提升生成质量
- **RouterNode**: 智能路由，根据需求自动选择代码生成类型
- **CodeGeneratorNode**: AI 代码生成（支持流式输出）
- **CodeQualityCheckNode**: 代码质量检查，不合格自动重生成
- **ProjectBuilderNode**: Vue 项目自动构建

### 3. 代码质量检查

AI 自动质检生成的代码，检查语法错误、逻辑问题、安全性等，质检不合格自动重新生成。

### 4. 流式输出

代码生成过程支持流式响应，实时展示生成进度，提升用户体验。

### 5. 应用管理

- 创建和管理 AI 应用
- 自定义应用封面和初始化 Prompt
- 应用部署与分享
- 优先级排序

### 6. 用户系统

- 用户注册/登录
- 基于 Session 的身份认证
- 角色权限控制（普通用户 / 管理员）
- AOP 切面权限校验

### 7. 对话历史

- 保存用户与 AI 的对话记录
- 支持历史对话查询和恢复

### 8. 分布式限流

基于 Redisson 实现的分布式限流，支持多种限流策略，保障系统稳定性。

## 项目结构

```
fu-ai-code/
├── frontend/                 # 前端项目
│   ├── src/
│   │   ├── api/              # API 接口
│   │   ├── layout/           # 布局组件
│   │   ├── router/           # 路由配置
│   │   ├── store/            # Pinia 状态管理
│   │   ├── utils/            # 工具函数
│   │   ├── views/            # 页面组件
│   │   │   ├── admin/        # 管理后台页面
│   │   │   ├── Chat.vue      # 聊天页面
│   │   │   ├── Apps.vue      # 应用列表
│   │   │   └── ...
│   │   ├── App.vue
│   │   └── main.js
│   ├── package.json
│   └── vite.config.js
├── sql/                      # 数据库脚本
│   ├── app.sql
│   └── chat_history.sql
├── src/main/java/com/fu/fuaicode/
│   ├── ai/                   # AI 服务层
│   │   ├── guardrail/        # 输入护栏
│   │   ├── model/            # AI 模型
│   │   ├── tools/            # Function Calling 工具
│   │   └── ...
│   ├── annotation/           # 自定义注解
│   ├── aop/                  # AOP 切面
│   ├── common/               # 通用类
│   ├── config/               # 配置类
│   ├── constant/             # 常量
│   ├── controller/           # 控制器
│   ├── core/                 # 核心业务
│   │   ├── builder/          # 项目构建器
│   │   ├── parser/           # 代码解析器
│   │   ├── saver/            # 文件保存器
│   │   └── AiCodeGeneratorFacade.java
│   ├── exception/            # 异常处理
│   ├── langgraph4j/          # LangGraph4j 工作流
│   │   ├── node/             # 工作流节点
│   │   ├── state/            # 工作流状态
│   │   └── CodeGenWorkflow.java
│   ├── mapper/               # Mapper 接口
│   ├── manager/              # 第三方服务封装
│   ├── model/                # 数据模型
│   ├── ratelimit/            # 分布式限流
│   ├── service/              # 业务服务
│   ├── utils/                # 工具类
│   └── FuAiCodeApplication.java
├── src/main/resources/
│   ├── mapper/               # MyBatis XML
│   ├── prompt/               # System Prompt 模板
│   └── application.yml       # 应用配置
├── pom.xml
└── README.md
```

## 快速开始

### 环境要求

- JDK 21+
- Node.js 16+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.6+

### 数据库配置

1. 创建数据库：

```sql
CREATE DATABASE fu_ai_code DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行 SQL 脚本（`sql/` 目录下）创建数据表。

### 后端启动

1. 修改 `src/main/resources/application.yml` 中的数据库和 Redis 配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fu_ai_code?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password
```

2. 配置 AI 模型 API Key（在配置类中配置）。

3. 启动后端服务：

```bash
./mvnw spring-boot:run
```

后端服务默认运行在 `http://localhost:8124/api`

接口文档地址：`http://localhost:8124/api/doc.html`

### 前端启动

1. 进入前端目录：

```bash
cd frontend
```

2. 安装依赖：

```bash
npm install
```

3. 启动开发服务器：

```bash
npm run dev
```

前端默认运行在 `http://localhost:5173`

## 核心模块说明

### 代码生成门面 - AiCodeGeneratorFacade

统一的代码生成入口，封装了代码生成、解析、保存的完整流程，支持同步和流式两种方式。

### 工作流引擎 - CodeGenWorkflow

基于 LangGraph4j 实现的有状态工作流，支持条件分支、循环重试等复杂编排。每个节点独立职责，通过共享状态传递数据。

### 智能路由 - AiCodeGenTypeRoutingService

根据用户输入的需求描述，自动判断最合适的代码生成类型（HTML / 多文件 / Vue 项目）。

### 分布式限流 - RateLimitAspect

基于 Redisson 实现的注解式分布式限流，支持按 IP、用户等维度限流。

## API 接口

### 用户模块

- `POST /user/register` - 用户注册
- `POST /user/login` - 用户登录
- `POST /user/logout` - 用户登出
- `GET /user/get/login` - 获取当前登录用户

### 应用模块

- `POST /app/add` - 创建应用
- `POST /app/update` - 更新应用
- `POST /app/delete` - 删除应用
- `GET /app/get/vo` - 获取应用详情
- `GET /app/list/page/vo` - 分页获取应用列表

### 聊天模块

- `POST /chat` - 发送消息（流式）
- `GET /chat/history/list` - 获取聊天历史

### 管理模块

- `POST /admin/app/update` - 管理员更新应用
- `POST /admin/user/update` - 管理员更新用户
- `GET /admin/user/page` - 分页获取用户列表

## 开发说明

### 新增代码生成类型

1. 在 `CodeGenTypeEnum` 中新增枚举值
2. 实现对应的 `AiCodeGeneratorService`
3. 在 `AiCodeGeneratorServiceFactory` 中注册
4. 实现对应的 `CodeParser` 和 `CodeFileSaver`
5. 在 `CodeGenWorkflow` 中配置工作流节点

### 新增工作流节点

1. 在 `langgraph4j/node/` 包下创建节点类
2. 实现节点的 `create()` 方法
3. 在 `CodeGenWorkflow.createGraph()` 中添加节点和边

## License

MIT License
