# AI 编码约束规范（AI Coding Rules & Constraints）

> **关于文件名**：`AGENTS.md` 是 AI 编码工具（Trae / Codex / Copilot / Cursor / Claude Code / Windsurf 等）约定俗成的自动加载文件名，特指"给 AI Agent 的项目级指令"，并非泛指的代理文档。
>
> **本文用途**：AI 辅助编码的系统约束。AI Agent 在理解需求、生成代码、重构或架构设计前，必须遵守以下规则。
>
> **配套文档**：[ARCHITECTURE.md](./ARCHITECTURE.md) 为分层蓝图与标准代码，改动模块内部时按需查阅；[README.md](./README.md) 为人类读者导览，AI 无需读取。

---

## 1. 角色与架构总则

* **角色**：严格遵循整洁架构（Clean Architecture）与依赖倒置原则（DIP）的 Java 架构师。
* **依赖流向**：`Infrastructure` ➔ `Application` ➔ `Domain`，单向向内收敛。
* **分层职责与包结构**：见 [ARCHITECTURE.md §2](./ARCHITECTURE.md#2-规范化包结构蓝图)。

---

## 2. 硬性红线（DO NOT）

红线的统一判据是**依赖封闭**——内层不依赖外层，领域不依赖框架，模块不触碰他模块的实现细节。以下为该判据在各场景的落地：

* **Domain 零框架语义**：`domain/` 包不引入任何框架注解与框架包。ORM（`@Entity`、`@TableName`）、依赖注入（`@Component`、`@Autowired`）、事务（`@Transactional`）、校验（`@NotNull`）、序列化（`@JsonProperty`）等框架语义一律违规。唯一豁免：编译期生效、零运行时依赖、不携带框架语义的纯元数据注解（如 Lombok 基本注解）。
* **充血模型**：业务校验与状态变更内聚于 Domain Model / Domain Service；堆砌在 ApplicationService 或 Controller 中的贫血模型一律禁止。
* **数据模型隔离**：PO 止步于持久化层，Request/VO 止步于 Web 层；跨层传递必须经 `Assembler` / `Converter` 显式转换（见 §3）。
* **模块封装边界**：跨模块仅可依赖对方 `module/<name>/api/` 包**显式暴露的服务接口及契约 DTO（Command/Query/Result）**；api 包之外的任何类型（Domain Model、Repository、应用服务实现类、Mapper、PO 等）均为内部实现，禁止引用。数据表归属唯一模块，严禁跨模块直查。

---

## 3. 数据模型隔离

| 模型 | 包路径 | 职责 | 转换器 |
| :--- | :--- | :--- | :--- |
| **Request / Response / VO** | `infrastructure/web/` | HTTP/REST 接口交互 | `Assembler` ↔ Command/Domain |
| **Domain Model / Aggregate** | `domain/model/` | 充血业务模型与状态表达 | 核心实体，禁含 ORM/JSON 注解 |
| **PO (Persistent Object)** | `infrastructure/persistence/entity/` | 物理表 ORM 映射 | `Converter` ↔ Domain Model |
| **契约 DTO (Command/Query/Result)** | `module/<name>/api/` | 跨模块调用的稳定公开契约（record） | 应用服务实现类内聚转换 ↔ Domain Model |

> 契约 DTO 与 web 层词汇严格区分：Request/Response/VO 只在 HTTP 边界流转，Command/Query/Result 只在跨模块契约中流转。契约类型允许引用本模块 domain 值语义类型（枚举、`record` 强类型 ID、值对象），禁止暴露聚合根、Repository、PO。`api` 包是模块的公开目录（"读 api 包 = 理解模块"），对外稳定——任何修改按破坏性变更对待，内部各层重构不应波及 `api` 包签名。规范细节见 [ARCHITECTURE.md §2](./ARCHITECTURE.md#2-规范化包结构蓝图)。

### 典型代码对比

**❌ BAD：框架依赖泄漏至 Domain 层**

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

**✅ GOOD：充血领域模型**

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

    // 充血业务行为：业务规则校验失败抛领域业务异常（定义于 domain/exception/，见 §5）
    public void cancel() {
        if (this.status == OrderStatus.COMPLETED) {
            throw new OrderCancellationException("已完成的订单不允许取消");
        }
        this.status = OrderStatus.CANCELLED;
    }
}
```

---

## 4. 仓储与端口模式

* **仓储**：接口定义于 `domain/repository/`（纯 Java Interface）；实现位于 `infrastructure/persistence/impl/`，经 `Converter` 完成 PO 与领域模型互转。
* **出站端口**：接口定义于 `application/port/`；实现位于 `infrastructure/integration/`。

```text
domain/repository/OrderRepository.java   (Interface)
         ▲
         │ implements
infrastructure/persistence/impl/OrderRepositoryImpl.java (Spring @Repository + Mapper + Converter)
```

标准代码见 [ARCHITECTURE.md §3](./ARCHITECTURE.md#3-标准代码规范)。

---

## 5. 代码生成偏好

1. **依赖注入**：构造器注入或 Lombok `@RequiredArgsConstructor`，禁止字段 `@Autowired`。
2. **封装性**：聚合根与领域实体使用精细的 `@Getter`，禁止滥用 `@Data`。
3. **强类型 ID**：业务对象标识使用模块内 `record` 声明（如 `OrderId(Long value)`），标识值在紧凑构造器中快速失败 null。
4. **异常**：业务规则校验失败统一抛出 `domain/exception/` 下的领域业务异常。
5. **单元测试**：编写或修改 Domain Model / Domain Service 时，同步生成 JUnit 5 + AssertJ 单元测试。

---

## 6. 项目自定义规则

> 规则按价值从高到低排序：影响 AI 工作方式的元规则 > 架构判据 > 风格约定。

### 规则 1：AI 导航协议（增量上下文，禁止全量通读）

为控制上下文成本，AI 助手按需增量导航，禁止非必要的全量代码通读：

1. **理解模块对外能力**：只读 `module/<name>/api/` 包（服务接口 + 契约 DTO），即可回答"该模块能做什么、怎么调"；禁止为理解模块能力而通读其内部实现。
2. **改动模块内部**：按 [ARCHITECTURE.md §2](./ARCHITECTURE.md#2-规范化包结构蓝图) 的包结构按层定位目标文件。
3. **判据优先**：验证架构合规时以本文档判据为准（依赖封闭、api 包边界、数据模型隔离），不从代码反推规则。

### 规则 2：工具类防腐规范

全项目禁止在业务代码中直接使用第三方工具类（如 `StringUtils`、`CollectionUtils`、`DateUtils` 等）；统一经由本项目 `com.xingyun.template.shared.util` 防腐层（现有：StringUtils、CollectionUtils），其委托实现、方法契约与分层使用细则以各类 Javadoc 为准。核心思想：

* **边界层宽松**：外部数据边界层（Controller / DTO / MQ Listener / RPC Client / Job 入参）面对未校验的外部输入，可依赖防腐层提供 null 安全契约的方法。
* **内部层严格**：Service / Domain / Infrastructure 禁止隐式 null 吞没——预期非空则快速失败，已断言非空用 JDK 原生方法，可选语义显式表达。

### 规则 3：注释与命名

代码注释与 Javadoc 统一使用中文；命名遵循标准 Java 驼峰。

---

> **扩展位**：以上为模板预设规则。你可以在此追加项目自定义规则——按价值从高到低插入排序，写法遵循"通用判据 + 如 … 等"举例，一条规则只讲一个主题。

---

## 附录：设计取舍记录

以下为模板的思想层取舍，实现细节活在规范与代码 Javadoc 中，此处不录。对模板使用者：取舍有前提，若你的项目前提不同，可在知情后自行调整；对 AI：在本项目内不得无意识重提被弃方案。

* 防腐层的意义是让替换只发生在一处 —— 工具类委托成熟库，而非重写轮子
* 值对象只描述业务语义，技术设施需要什么由消费方自行声明 —— 不做无主的预设
* 每一处知识单一、明确、权威地表达 —— 复制它，就是把同一个改动承诺两遍
* 复用优先组合与值语义，不造共享父类 —— 继承耦合在需求变化时成为负担
* 新增一层之前，先说出它消除的具体痛点 —— 说不出的层一律不加
* 信任来自可掌握 —— 说明书宁短勿长，读者敢说"读完了"才敢用
* 规则写判据不写清单 —— 判据能推理到没见过的情况，清单只会腐烂
