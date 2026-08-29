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
* **依赖封闭**：最内层的 `domain` 不携带任何框架语义（持久化 / 序列化 / 依赖注入等），判据与红线见 [AGENTS.md §2](./AGENTS.md)。
* **约束归属**：全部架构红线、数据模型隔离（Request/VO ↔ Domain Model ↔ PO，经 `Assembler`/`Converter` 显式转换）、跨模块 `api` 契约边界与工具防腐规则，统一维护于 **[AGENTS.md](./AGENTS.md)**（含 Good vs Bad 代码正反例）。

---

## 2. 规范化包结构蓝图

系统按 **领域边界（Bounded Context）** 划分高层模块，模块内部遵循统一三层结构：

```text
com.xingyun.template
├── shared                                  // 全局共享层 (不含业务语义，供所有模块复用)
│   ├── domain                              // 全局通用值对象 (例如: DateRange, Money 等纯 Java 值对象)
│   └── util                                // 工具防腐层 (统一封装第三方工具库，依赖与类型不外泄，契约见 AGENTS.md §6 规则 2)
│
└── module                                  // 业务领域模块根目录
    └── [domain_name]                       // 具体业务领域 (例如: example, payment)
        ├── api                             // 🚪 [0. 公开契约层] 模块唯一对外入口 (跨模块仅可依赖本包，判据见 AGENTS.md §2)
        │   ├── ExampleApi                  // 对外服务接口 (应用用例的公开视图，实现类位于 application/service)
        │   └── XxxCommand / XxxQuery / XxxResult
        │                                   // 契约 DTO (record 实现，词根即分组，平铺不设子包)
        │
        ├── domain                          // 💎 [1. 核心领域层] (纯业务逻辑，依赖封闭)
        │   ├── model                       // 领域模型 / 聚合根 (包含核心业务行为与状态)
        │   ├── repository                  // 仓储接口契约 (纯 Interface，定义持久化能力)
        │   ├── service                     // 核心领域服务 (跨聚合根的纯业务逻辑)
        │   └── exception                   // 领域业务异常
        │
        ├── application                     // 🟧 [2. 应用编排层] (应用用例实现)
        │   ├── service                     // 应用服务实现类 (实现本模块 api 包接口，内聚契约 DTO ↔ 领域模型转换)
        │   └── port                        // 出站端口接口 (例如: PaymentPort, SmsPort)
        │
        └── infrastructure                  // 🟨 [3. 基础设施适配层] (技术细节实现)
            ├── persistence                 // 数据库持久化实现
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
                └── assembler               // Request/Response ↔ Command/Domain 转换器
```

---

## 3. 标准代码规范

以 `example` 模块为例，给出各层标准写法。

> 示例以 MyBatis-Plus 书写，从简以 `Long` 传递标识；正式契约推荐 record 强类型 ID，生成偏好见 [AGENTS.md §5](./AGENTS.md#5-代码生成偏好)。本模板不预设 ORM 依赖，引入其他框架时仅需调整 PO 注解，分层结构与转换契约不变。

### 3.1 领域层：充血聚合根 (`domain/model/ExampleModel.java`)

```java
package com.xingyun.template.module.example.domain.model;

import java.util.Objects;

import lombok.Getter;

/**
 * 示例聚合根：业务校验与状态变更内聚于此（充血模型，禁含框架注解）。
 */
@Getter
public class ExampleModel {

    private final Long id;
    private String name;
    private final String code;

    private ExampleModel(Long id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
    }

    /**
     * 创建聚合根：新聚合无主键，由仓储 save 后回填。
     */
    public static ExampleModel create(String name, String code) {
        Objects.requireNonNull(name, "name 不能为 null");
        Objects.requireNonNull(code, "code 不能为 null");
        return new ExampleModel(null, name, code);
    }

    /**
     * 从持久化状态重建聚合根：携带主键，供 Converter 从 PO 还原时调用。
     */
    public static ExampleModel reconstitute(Long id, String name, String code) {
        Objects.requireNonNull(id, "id 不能为 null");
        return new ExampleModel(id, name, code);
    }

    /**
     * 重命名：业务规则校验内聚于聚合根，失败抛领域业务异常（定义于 domain/exception/）。
     */
    public void rename(String newName) {
        if (name.equals(newName)) {
            throw new ExampleRenameException("新名称与当前名称相同");
        }
        this.name = newName;
    }
}
```

### 3.2 领域层：仓储契约 (`domain/repository/ExampleRepository.java`)

```java
package com.xingyun.template.module.example.domain.repository;

import com.xingyun.template.module.example.domain.model.ExampleModel;
import java.util.Optional;

/**
 * 示例聚合根的仓储契约（纯 Java Interface，仅依赖领域模型）。
 */
public interface ExampleRepository {
    Optional<ExampleModel> findById(Long id);

    /**
     * 保存聚合根，返回持久化后的领域模型（含回填的主键）。
     */
    ExampleModel save(ExampleModel exampleModel);
}
```

### 3.3 基础设施层：物理表 PO (`infrastructure/persistence/entity/ExamplePO.java`)

```java
package com.xingyun.template.module.example.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * t_example 表的物理表 PO，仅存在于基础设施持久化层，禁止外泄。
 */
@Data
@TableName("t_example")
public class ExamplePO {
    @TableId
    private Long id;
    private String name;
    private String code;
    private Integer status;
}
```

### 3.4 基础设施层：仓储实现 (`infrastructure/persistence/impl/ExampleRepositoryImpl.java`)

```java
package com.xingyun.template.module.example.infrastructure.persistence.impl;

import com.xingyun.template.module.example.domain.model.ExampleModel;
import com.xingyun.template.module.example.domain.repository.ExampleRepository;
import com.xingyun.template.module.example.infrastructure.persistence.converter.ExampleConverter;
import com.xingyun.template.module.example.infrastructure.persistence.entity.ExamplePO;
import com.xingyun.template.module.example.infrastructure.persistence.mapper.ExampleMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 仓储契约的基础设施实现：经 Converter 完成 PO 与领域模型互转后，委托 Mapper 持久化。
 */
@Repository
public class ExampleRepositoryImpl implements ExampleRepository {

    private final ExampleMapper exampleMapper;
    private final ExampleConverter exampleConverter;

    public ExampleRepositoryImpl(ExampleMapper exampleMapper, ExampleConverter exampleConverter) {
        this.exampleMapper = exampleMapper;
        this.exampleConverter = exampleConverter;
    }

    @Override
    public Optional<ExampleModel> findById(Long id) {
        ExamplePO po = exampleMapper.selectById(id);
        return Optional.ofNullable(exampleConverter.toDomain(po));
    }

    @Override
    public ExampleModel save(ExampleModel exampleModel) {
        ExamplePO po = exampleConverter.toPO(exampleModel);
        if (po.getId() == null) {
            // 新增：主键为数据库自增策略，insert 后由框架回填至 PO
            exampleMapper.insert(po);
        } else {
            exampleMapper.updateById(po);
        }
        // 将持久化结果（含回填主键）转回领域模型，保证调用方拿到与存储一致的聚合状态
        return exampleConverter.toDomain(po);
    }
}
```

### 3.5 公开契约层：对外契约与应用服务实现 (`api/`、`application/service/`)

单契约模式：`api` 包的服务接口即应用服务接口，实现类位于 `application/service`；契约 DTO 为 `record`（如 `ExampleCreateCommand(String name, String code)`），词根即分组、平铺不设子包，判据见 [AGENTS.md §3](./AGENTS.md#3-数据模型隔离)。

```java
package com.xingyun.template.module.example.api;

import java.util.Optional;

/**
 * 示例模块对外契约：本模块全部对外能力的唯一视图（跨模块调用仅可依赖本包）。
 */
public interface ExampleApi {

    Optional<ExampleResult> findById(Long id);

    ExampleResult create(ExampleCreateCommand command);
}
```

```java
package com.xingyun.template.module.example.application.service;

import com.xingyun.template.module.example.api.ExampleApi;
import com.xingyun.template.module.example.api.ExampleCreateCommand;
import com.xingyun.template.module.example.api.ExampleResult;
import com.xingyun.template.module.example.domain.model.ExampleModel;
import com.xingyun.template.module.example.domain.repository.ExampleRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * ExampleApi 的应用服务实现：编排用例流程与事务，内聚契约 DTO ↔ 领域模型转换。
 */
@Service
public class ExampleApiImpl implements ExampleApi {

    private final ExampleRepository exampleRepository;

    public ExampleApiImpl(ExampleRepository exampleRepository) {
        this.exampleRepository = exampleRepository;
    }

    @Override
    public Optional<ExampleResult> findById(Long id) {
        return exampleRepository.findById(id).map(ExampleApiImpl::toResult);
    }

    @Override
    public ExampleResult create(ExampleCreateCommand command) {
        ExampleModel model = ExampleModel.create(command.name(), command.code());
        return toResult(exampleRepository.save(model));
    }

    // 契约 DTO ↔ 领域模型转换内聚于实现类，单向小映射不为它增设独立转换器
    private static ExampleResult toResult(ExampleModel model) {
        return new ExampleResult(model.getId(), model.getName(), model.getCode());
    }
}
```
