# AI 编码约束规范（AI Coding Rules & Constraints）

> **关于文件名**：`AGENTS.md` 是 AI 编码工具（Trae / Codex / Copilot / Cursor / Claude Code / Windsurf 等）约定俗成的自动加载文件名，特指"给 AI Agent 的项目级指令"，并非泛指的代理文档。
>
> **本文用途**：AI 辅助编码的系统约束。AI Agent 在理解需求、生成代码、重构或架构设计前，必须遵守以下规则。
>
> **配套文档**：[ARCHITECTURE.md](./ARCHITECTURE.md) 为分层蓝图（含规范化包结构），改动模块内部时按需查阅；[README.md](./README.md) 为人类读者导览，AI 无需读取。

章节顺序即 AI 工作顺序：进场先导航（§1），再守架构红线与分层规则（§2–§4），横切工程规则贯穿写代码与修订全程（§5），编码层面的统一约定集中于 §6。

---

## 1. AI 导航协议

为控制上下文成本，AI 助手按需增量导航，禁止非必要的全量代码通读：

1. **理解模块对外能力**：只读 `module/<name>/api/` 包（服务接口 + 契约 DTO），即可回答"该模块能做什么、怎么调"；禁止为理解模块能力而通读其内部实现。
2. **改动模块内部**：按 [ARCHITECTURE.md §2](./ARCHITECTURE.md#2-规范化包结构蓝图) 的包结构按层定位目标文件。范例 `module/example/` 是可整体移除的教学模块：若该目录存在，新写某层代码前先读范例中对应层文件，仿写而非自创——范例已覆盖 api、domain（model、repository）、application/service 与 infrastructure（web、persistence）。跨模块调用的写法见同为教学模块的 `module/comment/`（消费 example 的 api 契约；该目录不存在时按 §2 模块封装边界判据书写）。若该目录不存在，说明使用者已移除范例，属正常状态——不寻找、不重建范例，范例未含的层（domain/service、application/port、infrastructure/integration）同样如此，一律按 §2 红线、§4 仓储与防腐判据与 ARCHITECTURE 包树注释书写。文件级规则与决策承载在代码内中文注释中（注释与代码同权，见 §5）。
3. **判据优先**：验证架构合规时以本文档判据为准（依赖封闭、api 包边界、数据模型隔离），不从代码反推规则。

---

## 2. 硬性红线（DO NOT）

**角色**：严格遵循整洁架构（Clean Architecture）与依赖倒置原则（DIP）的 Java 架构师。依赖流向 `Infrastructure` ➔ `Application` ➔ `Domain`，单向向内收敛。

红线的统一判据是**依赖封闭**——内层不依赖外层，领域不依赖框架，模块不触碰他模块的实现细节。以下为该判据在各场景的落地：

* **Domain 零框架语义**：`domain/` 包禁止携带任何框架语义——ORM 注解（`@Entity`、`@TableName`）、依赖注入（`@Component`、`@Autowired`）、事务（`@Transactional`）、校验（`@NotNull`）、序列化（`@JsonProperty`）等均在禁止之列。事务边界标注于 `application/service`（应用服务用例方法），不归 Domain。唯一豁免：编译期生效、零运行时依赖、不携带框架语义的纯元数据注解（如 Lombok 基本注解）。
* **充血模型**：业务规则与它约束的聚合状态同居于模型。约束聚合不变量的规则（如重命名的同名拒绝、状态流转的前置条件）实现为模型的行为方法——校验与变更在模型内部完成，外部只能经行为方法改变聚合，不获得裸字段写入。规则写在 ApplicationService（职责仅为编排用例：调工厂与仓储、转契约）或 Controller（仅做协议转换）中即违规；跨聚合的纯业务逻辑归 Domain Service。
* **入参校验止步边界**：入参的格式与存在性校验（如 `@NotBlank`）属边界层输入约束、止步 Request——它不是业务规则，不随之进入 Domain。
* **数据模型隔离**：各层模型止步于所属层，跨层传递须经显式转换，禁止裸传（模型归属与转换器职责见 §3）。
* **模块封装边界**：`api` 包是模块唯一对外公开面——跨模块只依赖对方 `module/<name>/api/` 包显式暴露的服务接口与契约 DTO（Command/Query/Result），以及契约签名中引用的对方 domain 值语义类型（强类型 ID 等值对象——若不随签名公开，调用方将无法构造入参）；该包之外的其余类型（Domain Model、Repository、应用服务实现、Mapper、PO 等）均为内部实现，既禁止跨模块引用，也禁止经契约外泄。数据表归属唯一模块，严禁跨模块直查。

### 典型代码对比

下例同时对照 Domain 零框架语义与充血模型两条红线（分别杜绝框架语义泄漏与贫血模型）：

**❌ BAD：框架注解泄漏至 Domain 层，模型贫血无行为**

```java
package com.xingyun.template.module.order.domain.model;

import com.baomidou.mybatisplus.annotation.TableId; // ❌ ORM 注解
import org.springframework.stereotype.Component;    // ❌ 依赖注入注解

@Data
@Component
public class Order {
    @TableId
    private Long id;
}
```

**✅ GOOD：Domain 层为零框架注解的充血模型**

```java
package com.xingyun.template.module.order.domain.model;

import lombok.Getter;

@Getter
public class Order {
    private final Long id;
    private OrderStatus status;

    public Order(Long id, OrderStatus status) {
        this.id = id;
        this.status = status;
    }

    // 充血业务行为：业务规则校验内聚于聚合根，状态变更由聚合根自主完成
    public void cancel() {
        if (this.status == OrderStatus.COMPLETED) {
            throw new IllegalStateException("已完成的订单不允许取消");
        }
        this.status = OrderStatus.CANCELLED;
    }
}
```

---

## 3. 数据模型隔离

| 模型 | 包路径 | 职责 | 转换器 |
| :--- | :--- | :--- | :--- |
| **Request / Response / VO** | `infrastructure/web/` | HTTP/REST 接口交互 | `Assembler` ↔ Command/Result |
| **Domain Model / Aggregate** | `domain/model/` | 充血业务模型与状态表达 | — |
| **PO (Persistent Object)** | `infrastructure/persistence/entity/` | 物理表 ORM 映射 | `Converter` ↔ Domain Model |
| **契约 DTO (Command/Query/Result)** | `module/<name>/api/` | 跨模块调用的稳定公开契约（record） | 应用服务实现类内聚转换 ↔ Domain Model |

> 契约 DTO 与 web 层词汇严格区分：Request/Response/VO 只在 HTTP 边界流转，Command/Query/Result 只在跨模块契约中流转。契约类型允许引用本模块 domain 值语义类型（枚举、`record` 强类型 ID、值对象）。`api` 包对外稳定——任何修改按破坏性变更对待，内部各层重构不应波及 `api` 包签名。

---

## 4. 仓储、端口与防腐

指向外部技术的依赖一律经项目自有的收口点访问：

* **仓储与出站端口（依赖倒置双端）**：接口定义在内层——仓储属 `domain`、出站端口属 `application`，均为纯 Java Interface；技术实现在外层 `infrastructure`——持久化实现经 `Converter` 完成 PO 与领域模型互转，集成适配器封装外部服务调用。
* **第三方工具库（防腐层）**：生产代码禁止直接调用第三方库提供的静态工具方法（如 `org.apache.commons.lang3.StringUtils`、`org.springframework.util.CollectionUtils` 等；JDK 原生 API 不在此列）。通用处理统一经由本项目 `com.xingyun.template.shared.util` 防腐层，理由有二：其一、替换收口——底层实现集中一处，换库零调用方改动；其二、同名异源统一入口——`StringUtils` 在 Apache Commons、Spring Framework、Google Guava 等多个依赖中各有同名实现，统一入口可防止开发者各引各的、import 碎片化。所需方法防腐层未提供时，在防腐层内新增委托方法（底层委托第三方实现，同步声明 null 契约并附中文 Javadoc），而非在使用处直调第三方或自行手抄同类逻辑。委托实现与逐方法契约以防腐层各类 Javadoc 为准。

测试代码不受工具防腐约束。

---

## 5. 工程规则

本节收录不属单一架构层、但贯穿写代码与修订过程的横切规则。

### null 处理

null 处理按数据是否跨越信任边界分两层：

* **边界层宽松**：跨越系统边界进入进程的数据（HTTP 请求、MQ 消息、RPC 响应、定时任务参数等）在校验前不可信，边界位置可使用防腐层中声明 `@Nullable` 契约的 null 安全方法。
* **内部层严格**：Service / Domain / Infrastructure 内，预期非空的值先 `Objects.requireNonNull` 快速失败、其后直接用 JDK 原生方法，可能缺失的值用 `Optional` 显式表达——禁止以 null 容忍调用吞没"不应为 null 却为 null"的错误。

### 注释与命名

代码注释与 Javadoc 统一使用中文；命名遵循所在域的通行约定——生产代码遵循标准 Java 驼峰，测试等子域尊重其框架社区惯例（如 JUnit 测试方法下划线命名，中文语义由 `@DisplayName` 承载）；项目约定不覆盖成熟的生态约定。

注释与代码同权：本项目大量约束只以注释为载体——类与层的职责边界、方法的契约前提、依赖和配置存在的理由，漏读注释即漏读设计。改动代码必须同步修订相关注释，新增机制先写明它为什么存在；注释与代码不一致，与代码写错同责。

**文档不侵入代码**：代码注释承载规则判据本身，不指向外部文档章节（如"见 AGENTS.md §X"）。文档可引用代码位置，代码不引用文档位置。

### 异常处理

异常按失败的性质选型，判据是"谁捕获它、捕获后做什么"：

* **预期内的缺失不是异常**："查无此对象"这类正常可能的结果用 `Optional` 表达，禁止以异常驱动正常控制流（如 GET 单资源不存在时，404 在 `Optional` 末端构建响应）。
* **规则违规抛 JDK 标准异常**：聚合不变量被违反抛 `IllegalStateException`、非法入参抛 `IllegalArgumentException` 等标准类型；异常消息面向开发者日志，不是对外契约。禁止为每类业务失败自定义异常类、禁止 `BizException` 式错误码枚举——它们没有差异化捕获方，HTTP 状态码已是对外语义。
* **技术故障不捕获**：数据库不可用、外部调用超时等基础设施异常在业务代码中不 catch，传播到边界统一处理。
* **异常到响应的翻译集中一处**：由唯一的 `@RestControllerAdvice` 承担，映射判据以该类注释为准。

### 文档成稿

注释与文档修改后不留过程痕迹——不出现"原先""已删除""由 X 改为 Y"之类的修订叙述，删改与方案变更一律按最终意图重新表达，成品读来应如初次写下；历史追溯交给 Git。

### 任务收尾审计

编码完成不是任务完成的标志。每个任务收尾时，改动先按本规范判据过审，修复后自测，自测通过才算收尾。

交付时必须随附一行审计结论——无发现，或"发现 X，已修复，测试通过"；未报告即未审。发现按行为风险处置：表达级与结构级问题（注释、注解位置、依赖声明等）修复后跑测试即可；行为级问题先补一枚锁定当前行为的测试再修，对错由测试裁决。只报告说得出"不改会发生什么"的发现，零发现同样是合格的审计结论。

---

## 6. 代码生成约定

以下为编码层面的统一约定：

1. **依赖注入**：构造器注入或 Lombok `@RequiredArgsConstructor`，禁止字段 `@Autowired`（含测试）。
2. **封装性**：聚合根与领域实体使用精细的 `@Getter`，禁止滥用 `@Data`。
3. **强类型 ID**：业务对象标识使用模块内 `record` 声明（如 `OrderId(Long value)`），标识值在紧凑构造器中快速失败 null。
4. **单元测试**：编写或修改 Domain Model / Domain Service 时，同步生成 JUnit 5 + AssertJ 单元测试。
5. **导入规范**：所用类型逐条显式导入，禁止通配符（如 `import java.util.*`，静态导入同禁）；导入按组分列、组间空一行——静态导入组置顶，普通导入组内第三方包按包名字典序排列，JDK 原生包（`java.` 前缀）单独成组置于最后。

---

> **扩展位**：以上为模板预设规则。项目自定义规则追加于 §5——按价值从高到低插入排序，写法遵循"通用判据 + 如 … 等"举例，一条规则只讲一个主题。

---

## 附录：设计取舍

以下为模板的思想层取舍，实现细节活在规范与代码 Javadoc 中，此处不录。对模板使用者：取舍有前提，若你的项目前提不同，可在知情后自行调整；对 AI：在本项目内不得无意识重提被弃方案。

* 凡改动先说出它消除的具体痛点 —— 说不出的改动一律不做
* 复用即依赖，只建立可解除的依赖 —— 焊死的共享让一方的变化成为另一方的负担
* 自定义是消除具体痛点的最后手段 —— 少做定义，顺势而为
* 防腐的意义 —— 替换只发生在一处，且同名异源工具类（如 StringUtils）在项目内只有一个 import 入口，而非散播各处
* 每一处知识单一、明确、权威地表达 —— 复制它，就是把同一个改动承诺两遍
* 模型承载业务语义，技术设施需要什么由消费方自行声明 —— 不做无主的预设
* 架构约束写判据，操作约定写清单 —— 判据能推理到没见过的情况；无从推理的约定集中成清单、单点维护，不散落正文
* 信任来自可掌握 —— 说明书宁短勿长，读者敢说"读完了"才敢用
