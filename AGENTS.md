# AI 编码约束规范

> 本文件定义 AI Agent 在本仓库中的工作方式、架构边界与工程约束。AI Agent 在理解需求、架构设计、生成代码或重构前，必须遵守以下规则。
>
> 架构蓝图见 [ARCHITECTURE.md](./ARCHITECTURE.md)。
> 项目使用方式、模板初始化与运行说明见 [README.md](./README.md)。
> 示例模块及其用途见 [docs/examples.md](./docs/examples.md)。

## 1. 工作方式

章节顺序即 AI 工作顺序：先按 §1 导航并确定任务范围，再依据 §2–§4 处理架构与边界；§5 的工程规则和 §7 的编码约定贯穿实现过程；遇到未被具体条款覆盖的设计问题时依据 §9 判断；完成实现后按 §6 进行验证与收尾；文档变更遵循 §8。

### 1.1 任务范围

开始任务时，应先确定需求涉及的模块、层次与行为范围，并检查当前工作区状态。

不得覆盖、回退或清理用户已有修改。

读取代码应按需增量进行：理解模块对外能力时，先阅读目标模块的 `api/`；修改模块内部时，再按 [ARCHITECTURE.md](./ARCHITECTURE.md) 的包结构定位相关实现与测试。

实现方案按以下优先级确定：

1. 优先参考 `module/example/`、`module/comment/` 等示例模块；新增代码时，优先阅读对应层的范例。
2. 示例无法覆盖时，根据本文档与 [ARCHITECTURE.md](./ARCHITECTURE.md) 的规则推理。
3. 前两者无法确定时，再参考现有同类实现；现有实现不得视为架构规范或默认正确。

示例模块属于参考实现，可以被完整删除；示例不存在时，不应恢复示例代码作为前提。任务涉及具体文件时，应同时阅读其中与当前修改相关的注释和 Javadoc。

### 1.2 事实与规则

不同信息来源的职责不同：

* `AGENTS.md`：AI 工作规则与项目级工程约束。
* `ARCHITECTURE.md`：架构分层、依赖方向与包结构。
* 代码与 Javadoc：当前实现的局部事实与设计意图。
* 测试：当前行为的可执行约束。
* `README.md`：项目使用与运行说明。
* `docs/examples.md`：示例模块说明。

架构判断以 `AGENTS.md` 与 `ARCHITECTURE.md` 的判据为准，代码不得反向定义架构规则；当前行为以代码和测试为事实依据。

当规范、实现和测试存在不一致时，应识别实际冲突并保持修改范围可控，不得通过猜测掩盖冲突。

### 1.3 变更原则

只修改完成任务所必需的内容。

不得为了统一风格进行无关重构、批量迁移、目录重排或依赖替换。

新增抽象、共享组件或防腐层时，必须存在明确的职责、真实的复用或明确的解耦收益；不得为了预留未来需求提前抽象。

---

## 2. 架构边界

本项目采用 Clean Architecture、Hexagonal Architecture 与依赖倒置原则。

依赖方向必须保持向内收敛：

```text
Web / Infrastructure
        ↓
    Application
        ↓
      Domain
```

统一判据是**依赖封闭**：

> 内层不依赖外层；领域不依赖技术框架；模块不依赖其他模块的实现细节。

### 2.1 Domain

`domain/` 只承载领域模型、领域规则及其必要抽象，例如 Repository 接口，禁止携带框架语义，包括 ORM、依赖注入、事务、校验、Web、序列化等框架能力。

事务边界位于 `application/service`，不归 Domain。

允许使用不改变领域语义的编译期增强，如 Lombok 基本注解。

### 2.2 领域行为

聚合和实体负责维护自身状态与业务不变量。

约束聚合不变量的校验与状态变更必须在模型内部完成，外部只能通过领域行为修改聚合，不得绕过模型直接写入状态。

跨多个聚合的纯业务规则使用 Domain Service。

需要数据库、远程服务、当前用户、系统时间等外部事实的逻辑，不直接放入聚合模型。

Application Service 负责用例编排，不承载聚合内部业务规则；Controller 只负责 HTTP 协议适配、输入校验与契约转换。

### 2.3 输入校验

边界层负责输入自身的协议约束，包括必填性、类型、格式、长度与基本范围。

业务对象是否存在、当前状态是否允许操作、领域不变量是否满足，由 Application 与 Domain 负责判断。

### 2.4 模块封装

`module/<name>/api/` 是模块唯一公开面。

跨模块只能依赖对方 `api/` 中显式公开的服务接口、Command / Query / Result，以及契约签名所需的稳定值语义类型。

禁止跨模块引用或间接泄漏 Domain Model、Repository、Application Service 实现、Mapper、PO、Converter 等内部实现。

数据表归属单一模块，禁止跨模块直接访问其他模块的数据表。

### 2.5 模型边界

各层模型只属于其职责所在边界，跨层传递必须经过显式转换，不得裸传；具体模型归属与转换职责见 §3。

---

## 3. 模块公开面与模型隔离

### 3.1 模块公开面

`module/<name>/api/` 是模块唯一公开面。

跨模块只能依赖对方 `api/` 中明确公开的契约。

禁止跨模块直接依赖：

* Domain Model
* Repository
* Application Service 实现
* Mapper
* PO
* Converter
* Infrastructure 实现

模块内部实现对其他模块不可见。

数据表归属单一模块，禁止跨模块直接访问其他模块的数据表。

### 3.2 API 契约

`api/` 中主要承载：

* 模块公开服务接口
* Command
* Query
* Result
* 稳定的公开值语义类型

Request / Response / VO 只在 HTTP 边界流转，Command / Query / Result 只在模块公开契约中流转，二者不得混用。

公开契约应保持稳定。删除或修改字段、类型、必填性、枚举语义以及方法参数或返回值语义等，均按契约变更处理，并检查调用方影响。

### 3.3 公开值类型

可以在公开契约中使用稳定、无业务行为的值语义类型。

不得将聚合、实体或包含内部业务行为的 Domain 对象直接公开为模块间契约。

### 3.4 模型与组件

| 场景           | 职责 / 组件                  | 位置                                     | 触发条件           | 关系                       |
|--------------|--------------------------|----------------------------------------| -------------- | ------------------------ |
| HTTP 输入      | Request                  | `infrastructure/web/request`           | 有 HTTP 输入      | → Command / Query        |
| HTTP 输出      | Response / VO            | `infrastructure/web/response`          | 有 HTTP 输出      | Result →                 |
| HTTP 模型转换    | Assembler                | `infrastructure/web/assembler`         | 有 HTTP 模型转换时 | Web Model ↔ API Contract |
| 模块操作         | Command                  | `api`                                  | 有公开操作          | → Application            |
| 模块查询         | Query                    | `api`                                  | 有公开查询          | → Application            |
| 模块输出         | Result                   | `api`                                  | 公开能力需要输出       | Domain →                 |
| 领域业务模型       | Domain Model / Aggregate | `domain/model`                         | 有业务状态或规则       | 不跨边界裸传                   |
| 跨聚合规则        | Domain Service           | `domain/service`                       | 无法归属单一 Model   | 聚合 / 值对象                 |
| 持久化抽象        | Repository               | `domain/repository`                    | 需要持久化          | Domain ↔ Impl            |
| 用例实现         | Application Service      | `application/service`                  | 有用例编排          | Contract ↔ Domain        |
| 出站能力抽象       | Port                     | `application/port`                     | 需要隔离外部能力       | Application ↔ Adapter    |
| 数据库模型        | PO                       | `infrastructure/persistence/entity`    | 有持久化模型         | PO ↔ Domain              |
| PO 转换        | Converter                | `infrastructure/persistence/converter` | 有 PO 与 Domain Model 转换时 | PO ↔ Domain |
| Repository 实现 | Impl                     | `infrastructure/persistence/impl`      | 有 Repository   | → Mapper                 |
| 外部系统适配       | Adapter                  | `infrastructure/integration`           | 有外部系统          | Vendor ↔ Project         |
| 通用工具         | Shared Utility           | `shared/util`                          | 有真实跨模块复用       | 第三方 ↔ Project            |

### 3.5 模型隔离与转换

不同层模型不得直接裸传，必须在边界处完成显式转换。

典型关系：

```text
Request / Response
        ↕
     Assembler
        ↕
Command / Query / Result
        ↕
Application Service
        ↕
    Domain Model
        ↕
     Converter
        ↕
        PO
```

Web 模型与契约模型的转换由 Web Assembler 负责；契约模型与 Domain Model 的转换内聚于 Application Service；PO 与 Domain Model 的转换由 Persistence Converter 负责，不在 Repository 实现中直接完成。

简单映射可以内聚在所属组件；转换形成独立复杂逻辑或出现明确复用需求时，再提取专门组件。

---

## 4. Repository、Port 与外部依赖

指向外部技术的依赖必须经项目自有的边界进入内层。

### 4.1 Repository

Repository 表达领域持久化能力。

接口位于 `domain/repository`，使用纯 Java Interface；实现位于 Infrastructure。

持久化实现通过 Converter 完成 PO 与 Domain Model 的转换。

Domain 不依赖具体数据库、ORM、Mapper 或其他持久化框架。

### 4.2 Application Port

Port 表达 Application 所需要的非持久化外部能力。

接口位于 `application/port`，使用纯 Java Interface；实现位于 Infrastructure。

外部服务、消息、支付、文件存储等技术调用由 Integration Adapter 封装，不将 Vendor 类型与协议细节带入 Application 或 Domain。

### 4.3 第三方依赖与防腐

第三方依赖是否需要防腐，以其是否进入稳定业务语义、公开契约或形成明显替换与治理成本为判断依据。

需要防腐时，在合适边界建立项目自己的抽象；第三方对象、协议模型与技术语义不得进入 Domain 或模块公开契约。

对于已经建立的第三方工具防腐层，生产代码不得绕过防腐层直接调用对应第三方工具。防腐层缺少所需能力时，在防腐层中新增委托方法，并同步明确其 null 契约与中文 Javadoc；调用方不得自行重复实现或直调第三方。

防腐层的具体行为以各方法 Javadoc 为准。测试代码不受第三方工具防腐约束。

### 4.4 Shared

`shared` 不承载具体业务领域。

只有真正跨模块共享、且具有稳定语义的能力才进入 `shared`。

不得因为未来可能复用而提前共享。

---

## 5. 工程规则

### 5.1 依赖注入

使用构造器注入。

允许使用 Lombok `@RequiredArgsConstructor` 简化构造器生成。

禁止字段注入。

### 5.2 null

null 处理以信任边界为界：

* 边界输入在校验前视为不可信，由边界负责处理非法 null。
* 进入内部后，对预期非空的值保持严格契约，必要时使用 `Objects.requireNonNull` 快速失败。
* 正常缺失状态使用 `Optional` 等显式语义表达。
* 不得通过 null 容忍调用掩盖内部契约错误。

### 5.3 异常

异常只用于表达异常路径，不用于正常控制流。

异常类型以“谁需要捕获它、捕获后做什么”为判据；没有差异化处理需求时，优先使用标准 JDK 异常表达通用程序错误与状态错误。

当不同失败需要不同的捕获、重试、补偿、降级或对外语义时，可以定义专用异常类型。

技术异常不得为了统一而无意义地捕获；只有需要恢复、重试、降级、异常转换或补充必要上下文时才捕获。

HTTP 异常映射集中处理，Controller 不重复实现全局异常转换；具体映射规则以异常处理组件的代码与 Javadoc 为准。

### 5.4 事务

默认以一个完整的 Application Service 用例作为事务边界。

Application Service 可以负责事务控制、仓储访问和用例编排。

Domain 不声明事务。

不得通过扩大本地事务边界解决跨系统一致性问题。

涉及远程调用、消息发送、重试、补偿或最终一致性时，应保持本地事务与外部系统边界清晰。

---

## 6. 测试与验证

### 6.1 验证原则

每次代码变更都必须执行与变更风险相匹配的验证；存在相关测试时，应优先运行受影响测试，并在必要时扩大验证范围。

验证以证明行为、边界与架构约束仍然成立为目的，不以机械追求测试数量或覆盖率为目标。

### 6.2 修改前验证

任务开始后，应了解相关测试和构建的当前状态。

若受影响的测试或验证在修改前已经失败，应区分已有失败与本次变更引入的问题，不得将基线问题归因于本次修改。

### 6.3 修改后验证

根据变更范围选择验证方式：

* Domain Model / Domain Service → 领域单元测试
* Application Service / 用例编排 → 用例测试
* Web 请求、响应或异常映射 → Web 层测试
* Persistence 查询、映射或数据库行为 → 持久化 / 集成测试
* 跨模块 API → 契约与调用行为测试
* 架构、包结构或依赖边界变化 → 编译、测试及架构约束检查

Domain Model / Domain Service 的单元测试使用 JUnit 5 与 AssertJ。

需要运行项目测试时，默认使用 Maven：

```bash
mvn test
```

需要完整构建时：

```bash
mvn clean package
```

能够自动验证的内容应优先自动验证。

### 6.4 任务收尾审计

编码完成不是任务完成的标志。收尾时应审计本次变更对 §2–§5 以及 §7 的遵循情况，并确认 §6 所要求的验证已经完成；发现问题应修复后重新验证。

审计至少检查：

* diff 是否仅包含任务相关修改
* 模块 `api` 是否仍为唯一公开面
* Domain 是否保持框架无关
* DTO、PO、Domain Model 是否正确隔离
* 是否新增无必要的共享抽象或技术耦合
* 是否遗漏受影响的测试、契约与转换
* 是否留下临时代码、无效 import 或未使用实现

发现问题时按行为风险处理：表达级与结构级问题修复后重新验证；行为级问题先补测试明确预期行为或复现缺陷，再修复实现，由测试验证结果。

交付时必须给出一行审计结论：无发现，或说明发现的问题、修复结果与验证结果。只报告能够说明“不修改会发生什么”的有效发现，零发现同样是合格的审计结论。

无法执行必要验证时，应明确说明未验证项及原因，不得将未验证结果表述为通过。

---

## 7. 编码约定

> 本章定义默认的编码习惯与代码生成偏好，可按项目需要调整或扩展。
>
> 本章约定仅属于编码约定层。AI 应根据约定实际影响的范围判断其适用层级；若内容实际涉及架构边界、模块封装、模型隔离、外部依赖或其他上位工程规则，则以上位规则为准。发生冲突时，不得仅因约定位于本章而覆盖上位规则，并应告知用户该约定不生效及其原因。

### 7.1 Domain 封装

领域对象保持明确的状态边界。

聚合根与领域实体按需使用 `@Getter`，不生成无差别 setter；禁止使用 `@Data`。

状态变化优先通过领域行为方法完成，不通过公开 setter 暴露状态变更。

### 7.2 强类型 ID

具有稳定业务语义的对象标识优先使用本模块自己的 `record` 值类型。

例如：

```java id="n8o4dz"
public record OrderId(Long value) {
    public OrderId {
        Objects.requireNonNull(value, "value must not be null");
    }
}
```

纯技术用途的 ID 不必为了形式统一而强行包装。

### 7.3 注释与 Javadoc

项目注释和 Javadoc 使用中文。

命名遵循所在语言、框架与测试生态的通行约定：生产代码遵循标准 Java 命名惯例，测试等子域尊重其框架社区惯例；项目约定不无必要地覆盖成熟生态惯例。

注释用于表达代码本身无法充分体现的内容，例如：

* 类与层的职责边界
* 方法的契约与前置条件
* 业务不变量
* 非显然的设计原因
* 外部依赖或配置存在的理由

新增机制应说明其存在的必要性。

实现变化后，受影响的注释与 Javadoc 必须同步保持准确。代码注释表达局部设计意图，不通过“见 AGENTS.md §X”等方式替代说明；项目级规则仍以 `AGENTS.md` 与 `ARCHITECTURE.md` 为准。文档可以引用具体代码位置，代码不依赖文档章节才能成立。

### 7.4 代码组织

新增代码应保持职责、依赖与处理逻辑清晰。

当构造器依赖、方法参数、连续赋值或分支逻辑持续增长并影响可读性时，不应机械继续堆叠，应检查是否存在职责拆分、辅助方法或独立组件的需要。

新增成员、方法或逻辑应放在与其职责相符的位置，并保持局部结构一致。

### 扩展位

从这里开始增加项目自身的编码习惯与代码生成偏好。

新增约定仍属于第 7 章的编码约定层，由 AI 根据其实际影响范围判断是否适用。

---

## 8. 文档

文档只描述最终状态和当前适用的设计。

不在当前文档中记录：

* 历史方案
* 已被删除的实现
* 修改过程
* 尝试失败的方案
* 临时决策过程

历史信息由 Git 保留。

每类知识保持单一权威来源：

```text
Agent 工作规则   → AGENTS.md
架构规则         → ARCHITECTURE.md
使用说明         → README.md
示例说明         → docs/examples.md
局部设计意图     → 代码 / Javadoc
行为约束         → 测试
```

同一规则只保留一个权威来源；其他位置仅作必要的引用或局部说明。

---

## 9. 设计判据

面对未被具体条款覆盖的设计问题，优先依据以下原则判断：

1. 依赖是否仍然向内收敛。
2. 内层是否引入了外层技术语义。
3. 模块是否暴露了实现细节。
4. 数据模型是否跨越了不属于它的边界。
5. 领域不变量是否仍由领域模型维护。
6. 是否为了复用建立了不必要的共享依赖。
7. 是否可以使用已有且语义匹配的抽象。
8. 新增结构是否具有明确的行为或解耦收益。

能够由现有架构判据直接得到的结论，不额外制造规则。

无法合理判断时，以最小变更、最小耦合和清晰边界为原则。
