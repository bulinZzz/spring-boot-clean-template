# Spring Boot 整洁架构蓝图

## 1. 架构总则

本蓝图基于**整洁架构（Clean Architecture）**与**六边形架构（Hexagonal Architecture）**，严格执行**依赖倒置原则（DIP）**。

### 分层与依赖方向

```text
┌─────────────────────────────────────────────────────────┐
│  Infrastructure / Web / Integration  (最外层：技术细节)  │
└───────────────────────────┬─────────────────────────────┘
                            │ 依赖 (Imports)
                            ▼
┌─────────────────────────────────────────────────────────┐
│        Application           (中间层：应用用例与流程编排) │
└───────────────────────────┬─────────────────────────────┘
                            │ 依赖 (Imports)
                            ▼
┌─────────────────────────────────────────────────────────┐
│          Domain              (最内层：纯粹业务逻辑核心)   │
└───────────────────────────┴─────────────────────────────┘
```

* **依赖单向**：外层适配器与技术实现依赖内层业务抽象，内层核心禁止依赖外层。
* **依赖封闭**：最内层的 `domain` 不携带任何框架语义（持久化 / 序列化 / 依赖注入等），红线见 [AGENTS.md §2](./AGENTS.md#2-硬性红线do-not)。
* **形状来源**：本图所有分层与包名的规则判据，维护于 [AGENTS.md](./AGENTS.md)。

---

## 2. 规范化包结构蓝图

系统按 **领域边界（Bounded Context）** 划分高层模块；模块内部遵循统一三层结构（domain / application / infrastructure），`api` 为模块唯一公开契约包：

```text
com.xingyun.template
├── Application.java                        // 🚀 Spring Boot 启动主类 (唯一启动类，组件扫描覆盖全部模块)
├── shared                                  // 全局共享层 (不含业务语义，供所有模块复用)
│   ├── domain                              // 跨模块共享的值对象 (例如: DateRange, Money；准入判据: ≥2 个模块真实消费)
│   ├── util                                // 工具防腐层 (统一封装第三方工具库，依赖与类型不外泄，判据见 AGENTS.md §4)
│   ├── integration                         // 跨模块共用技术连接器 (例如: Redis 连接装配；准入判据: ≥2 个模块真实消费)
│   └── web                                 // 全局 Web 设施 (例如: 异常→HTTP 翻译的 @RestControllerAdvice)
│
└── module                                  // 业务领域模块根目录
    └── [domain_name]                       // 具体业务领域 (例如: example, comment)
        ├── api                             // 🚪 [0. 公开契约包] 模块唯一对外入口 (跨模块仅可依赖本包，判据见 AGENTS.md §2)
        │   ├── ExampleApi                  // 对外服务接口 (应用用例的公开视图，实现类位于 application/service)
        │   └── XxxCommand / XxxQuery / XxxResult  // 契约 DTO (record 实现，按用例词根命名，平铺不设子包)
        │
        ├── domain                          // 💎 [1. 核心领域层] (纯业务逻辑，依赖封闭)
        │   ├── model                       // 领域模型 / 聚合根 (包含核心业务行为与状态)
        │   ├── repository                  // 仓储接口 (纯 Interface，定义持久化能力)
        │   └── service                     // 领域服务 (跨聚合根的纯业务逻辑)
        │
        ├── application                     // 🟧 [2. 应用编排层] (应用用例实现)
        │   ├── service                     // 应用服务实现类 (实现本模块 api 包接口，内聚契约 DTO ↔ 领域模型转换；用例方法即事务边界)
        │   └── port                        // 出站端口接口 (例如: PaymentPort, SmsPort)
        │
        └── infrastructure                  // 🟨 [3. 基础设施适配层] (技术细节实现)
            ├── persistence                 // 数据库持久化实现 (MyBatis-Plus；ORM 耦合点仅 PO 注解与 Mapper 声明，更换 ORM 不影响分层结构与转换契约)
            │   ├── entity                  // ORM 物理表 PO (携带框架注解)
            │   ├── mapper                  // 原生 Mapper / DAO 接口
            │   ├── converter               // PO ↔ 领域模型双向转换器
            │   └── impl                    // domain/repository 接口的实现类
            │
            ├── integration                 // 第三方服务适配器 (实现 application/port 接口)
            │   └── [vendor]                // 外部服务 SDK 调用与防腐封装
            │
            └── web                         // HTTP / REST 适配器 (入口)
                ├── controller              // Spring RestController
                ├── request                 // 入参 Request DTO (携带校验注解)
                ├── response                // 出参 Response VO
                └── assembler               // Request/Response ↔ Command/Result 转换器
```

**可运行范例**：各层标准写法以 `module/example/` 为完整范例（可整体移除的教学模块，含建表脚本与单元测试），跨模块调用的写法见 `module/comment/`，搭建业务模块时直接参照其包结构与代码；文件级规则与决策以代码内中文注释承载（注释与代码同权，见 [AGENTS.md §5](./AGENTS.md#5-工程规则)），架构判据见 [AGENTS.md](./AGENTS.md)。示例存储由内嵌 H2 开箱提供，其构成、使用与移除见 [docs/embedded-h2.md](./docs/embedded-h2.md)。


