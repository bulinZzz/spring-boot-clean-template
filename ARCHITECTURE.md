# Spring Boot 整洁架构设计规范

## 1. 架构总则

本规范基于**整洁架构（Clean Architecture）**与**六边形架构（Hexagonal Architecture）**，严格执行**依赖倒置原则（DIP）**。

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
* **依赖封闭**：最内层的 `domain` 不携带任何框架语义（持久化 / 序列化 / 依赖注入等），判据与红线见 [AGENTS.md §2](./AGENTS.md#2-硬性红线do-not)。
* **约束归属**：全部架构红线、数据模型隔离（Request/VO ↔ Domain Model ↔ PO，经 `Assembler`/`Converter` 显式转换）、跨模块 `api` 契约边界与工具防腐规则，统一维护于 **[AGENTS.md](./AGENTS.md)**（含 Good vs Bad 代码正反例）。

---

## 2. 规范化包结构蓝图

系统按 **领域边界（Bounded Context）** 划分高层模块；模块内部遵循统一三层结构（domain / application / infrastructure），`api` 为模块唯一公开契约包：

```text
com.xingyun.template
├── Application.java                        // 🚀 Spring Boot 启动主类（唯一启动类，组件扫描覆盖全部模块，新模块无需自建启动类）
├── shared                                  // 全局共享层 (不含业务语义，供所有模块复用)
│   ├── domain                              // 全局通用值对象 (例如: DateRange, Money 等纯 Java 值对象)
│   ├── util                                // 工具防腐层 (统一封装第三方工具库，依赖与类型不外泄，判据见 AGENTS.md §4)
│   └── integration                         // 跨模块共用技术连接器 (例如: Redis 连接装配；准入判据: ≥2 个模块真实消费)
│
└── module                                  // 业务领域模块根目录
    └── [domain_name]                       // 具体业务领域 (例如: example, payment)
        ├── api                             // 🚪 [0. 公开契约包] 模块唯一对外入口 (跨模块仅可依赖本包，判据见 AGENTS.md §2)
        │   ├── ExampleApi                  // 对外服务接口 (应用用例的公开视图，实现类位于 application/service)
        │   └── XxxCommand / XxxQuery / XxxResult
        │                                   // 契约 DTO (record 实现，按用例词根命名，平铺不设子包)
        │
        ├── domain                          // 💎 [1. 核心领域层] (纯业务逻辑，依赖封闭)
        │   ├── model                       // 领域模型 / 聚合根 (包含核心业务行为与状态)
        │   ├── repository                  // 仓储接口契约 (纯 Interface，定义持久化能力)
        │   └── service                     // 核心领域服务 (跨聚合根的纯业务逻辑)
        │
        ├── application                     // 🟧 [2. 应用编排层] (应用用例实现)
        │   ├── service                     // 应用服务实现类 (实现本模块 api 包接口，内聚契约 DTO ↔ 领域模型转换；用例方法即事务边界，@Transactional 标注于此层，domain 禁用见 AGENTS.md §2)
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

**可运行范例**：各层标准写法以 `module/example/` 为完整范例（可整体移除的教学模块，含建表脚本与单元测试），搭建业务模块时直接参照其包结构与代码；文件级规则与决策以代码内中文注释承载（注释与代码同权，见 [AGENTS.md §5](./AGENTS.md#5-工程规则)），架构判据见 [AGENTS.md](./AGENTS.md)。示例存储由内嵌 H2 开箱提供，其构成、使用与移除见文末附录。

---

## 附录：内嵌数据库（示例模块的开箱存储）

example 模块的仓储链路需要真实数据库才能跑通，但模板不预设使用者的数据库环境，也不应要求先安装、建库才能运行。模板内置了 **H2**——一个以纯 Java 实现、可随应用进程内嵌启动的轻量级数据库，并以**内存模式**运行：库表与数据只存活于应用进程内，启动时重建、停止时消失，零安装、零外部配置。

H2 让 `mvn spring-boot:run` 之后示例接口立即可用，是示例的运行期与开发期设施，不是生产存储选型。接入真实数据库或移除示例时，清理步骤见本附录「移除」。

### 构成

| 组成 | 位置 | 职责 |
| :--- | :--- | :--- |
| H2 驱动依赖 | `pom.xml`：`com.h2database:h2`（runtime 作用域） | 内嵌数据库的 JDBC 驱动 |
| 控制台依赖 | `pom.xml`：`org.springframework.boot:spring-boot-h2console` | H2 Web 控制台自动配置，缺它则 `spring.h2.console.enabled` 不生效 |
| 数据源配置 | `application.yml`：`spring.datasource` | 应用连接内存库的数据源；URL 中 `DB_CLOSE_DELAY=-1` 保证连接全部关闭后，库在应用运行期间不被销毁 |
| 控制台开关 | `application.yml`：`spring.h2.console.enabled` | 开启 H2 Web 控制台 |
| 建表脚本 | `src/main/resources/schema.sql` | `t_example` 建表 DDL；位于 classpath 根目录，Spring Boot 对内嵌库默认在启动时自动执行 |

持久化框架 MyBatis-Plus（`mybatis-plus-spring-boot4-starter`）与具体数据库无关，不属于 H2 组成——更换数据库时它保留。

### 使用：查看内存库数据

1. 启动应用，浏览器访问 `http://localhost:8080/h2-console`；
2. 登录页连接信息与 `application.yml` 的 `spring.datasource` 配置一致：JDBC URL 填内存库地址 `jdbc:h2:mem:example`（控制台连接不带 `DB_CLOSE_DELAY` 参数），用户名与密码照配置填写；
3. 连接后即可查看示例表（脚本中写作 `t_example`，H2 对未加引号的标识符按大写存储，控制台中显示为 `T_EXAMPLE`），调用 `POST /examples` 写入的数据即时可见。

内存库只存活于应用进程内：IDEA 数据库工具、DBeaver 等外部客户端无法连接——在另一个 JVM 中用同一 URL 只会新建一个同名的空库；应用停止，数据即失。

### 移除

H2 仅服务于示例开箱即跑。接入真实数据库时按下列步骤替换；不需要持久化示例时，H2 构件按同一步骤删除（example 模块整体移除见 README 快速开始）：

1. **依赖**：删除 `pom.xml` 中的 `h2` 与 `spring-boot-h2console`；接入 MySQL 等数据库时替换为对应驱动。
2. **配置**：`application.yml` 中 `spring.datasource` 替换为真实数据库连接，删除 `spring.h2.console` 配置段。
3. **脚本**：删除 `src/main/resources/schema.sql`——它是 H2 方言 DDL；真实库建表交由迁移工具（如 Flyway、Liquibase）或 DBA 流程，主键需为数据库自增列以匹配 PO 的 `IdType.AUTO`。

若项目随之不再需要持久化，`mybatis-plus-spring-boot4-starter` 一并删除——类路径上存在持久化 starter 却无数据源配置时，应用启动失败。
