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
│   ├── util                                // 工具防腐层 (统一封装第三方工具库，依赖与类型不外泄，契约见 AGENTS.md §6 规则 2)
│   └── integration                         // 跨模块共用技术连接器 (例如: Redis 连接装配；准入判据: ≥2 个模块真实消费)
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
        │   └── service                     // 核心领域服务 (跨聚合根的纯业务逻辑)
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
│               └── assembler               // Request/Response ↔ Command/Result 转换器
```

---

## 3. 标准代码规范

以 `example` 模块为例，给出各层标准写法（可运行的完整代码见 `module/example/`，含建表脚本与单元测试）。

> 示例与 `module/example/` 逐字镜像，修订先改本文档、代码跟随。持久化以 MyBatis-Plus 书写（示例存储使用内嵌数据库 H2，构成、使用与移除见文末附录）；更换其他 ORM 时仅需调整 PO 注解与 Mapper 声明，分层结构与转换契约不变。标识以 record 强类型 `ExampleId` 传递，生成偏好见 [AGENTS.md §5](./AGENTS.md#5-代码生成偏好)。

### 3.1 领域层：强类型标识 (`domain/model/ExampleId.java`)

```java
package com.xingyun.template.module.example.domain.model;

import java.util.Objects;

/**
 * 示例聚合的强类型标识：以专属类型隔离不同实体的裸值，杜绝标识混传。
 */
public record ExampleId(Long value) {

    public ExampleId {
        Objects.requireNonNull(value, "标识值不能为 null");
    }
}
```

### 3.2 领域层：充血聚合根 (`domain/model/ExampleModel.java`)

```java
package com.xingyun.template.module.example.domain.model;

import lombok.Getter;

import java.util.Objects;

/**
 * 示例聚合根：业务校验与状态变更内聚于此（充血模型，禁含框架注解）。
 */
@Getter
public class ExampleModel {

    private final ExampleId id;
    private final String code;
    private String name;

    private ExampleModel(ExampleId id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    /**
     * 创建聚合根：新聚合无主键，由仓储 save 后回填。
     */
    public static ExampleModel create(String code, String name) {
        Objects.requireNonNull(code, "code 不能为 null");
        Objects.requireNonNull(name, "name 不能为 null");
        return new ExampleModel(null, code, name);
    }

    /**
     * 从持久化状态重建聚合根：携带主键，供仓储实现还原聚合时调用。
     */
    public static ExampleModel reconstitute(ExampleId id, String code, String name) {
        Objects.requireNonNull(id, "id 不能为 null");
        return new ExampleModel(id, code, name);
    }

    /**
     * 重命名：同名拒绝。
     */
    public void rename(String newName) {
        if (name.equals(newName)) {
            throw new IllegalStateException("新名称与当前名称相同");
        }
        this.name = newName;
    }
}
```

### 3.3 领域层：仓储契约 (`domain/repository/ExampleRepository.java`)

```java
package com.xingyun.template.module.example.domain.repository;

import com.xingyun.template.module.example.domain.model.ExampleId;
import com.xingyun.template.module.example.domain.model.ExampleModel;

import java.util.Optional;

/**
 * 示例聚合根的仓储契约（纯 Java Interface，仅依赖领域模型）。
 */
public interface ExampleRepository {

    /**
     * 按标识查询聚合根，不存在时返回 {@code empty}。
     */
    Optional<ExampleModel> findById(ExampleId id);

    /**
     * 保存聚合根，返回持久化后的领域模型（含回填的主键）。
     */
    ExampleModel save(ExampleModel exampleModel);
}
```

### 3.4 基础设施层：物理表 PO (`infrastructure/persistence/entity/ExamplePO.java`)

```java
package com.xingyun.template.module.example.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * t_example 表的物理表 PO，仅存在于基础设施持久化层，禁止外泄。
 */
@Getter
@Setter
@TableName("t_example")
public class ExamplePO {

    // 主键由数据库自增生成，insert 后由框架回填（不指定时 MyBatis-Plus 默认走雪花算法）
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
}
```

### 3.5 基础设施层：Mapper 与转换器 (`persistence/mapper/`、`persistence/converter/`)

```java
package com.xingyun.template.module.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xingyun.template.module.example.infrastructure.persistence.entity.ExamplePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * t_example 表的 Mapper：继承 BaseMapper 获得通用 CRUD，不写 SQL。
 */
@Mapper
public interface ExampleMapper extends BaseMapper<ExamplePO> {
}
```

```java
package com.xingyun.template.module.example.infrastructure.persistence.converter;

import com.xingyun.template.module.example.domain.model.ExampleId;
import com.xingyun.template.module.example.domain.model.ExampleModel;
import com.xingyun.template.module.example.infrastructure.persistence.entity.ExamplePO;
import org.springframework.stereotype.Component;

/**
 * ExamplePO ↔ ExampleModel 双向转换器：标识值在此完成 Long ↔ ExampleId 互转。
 */
@Component
public class ExampleConverter {

    /**
     * PO 转领域模型：要求 PO 已携带主键（insert 回填后调用）。
     */
    public ExampleModel toDomain(ExamplePO po) {
        return ExampleModel.reconstitute(new ExampleId(po.getId()), po.getCode(), po.getName());
    }

    /**
     * 领域模型转 PO：新聚合的空标识映射为 null 主键，交给数据库自增。
     */
    public ExamplePO toPO(ExampleModel model) {
        ExamplePO po = new ExamplePO();
        po.setId(model.getId() == null ? null : model.getId().value());
        po.setCode(model.getCode());
        po.setName(model.getName());
        return po;
    }
}
```

### 3.6 基础设施层：仓储实现 (`infrastructure/persistence/impl/ExampleRepositoryImpl.java`)

```java
package com.xingyun.template.module.example.infrastructure.persistence.impl;

import com.xingyun.template.module.example.domain.model.ExampleId;
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
    public Optional<ExampleModel> findById(ExampleId id) {
        ExamplePO po = exampleMapper.selectById(id.value());
        // 未命中直接返回 empty：Converter 契约要求 PO 非空，判空责任在仓储
        return po == null ? Optional.empty() : Optional.of(exampleConverter.toDomain(po));
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

### 3.7 公开契约层：对外契约与应用服务实现 (`api/`、`application/service/`)

单契约模式：`api` 包的服务接口即应用服务接口，实现类位于 `application/service`；契约 DTO 为 `record`、词根即分组、平铺不设子包，出参以强类型标识出契约，允许引用本模块 domain 值语义类型，判据见 [AGENTS.md §3](./AGENTS.md#3-数据模型隔离)。

```java
package com.xingyun.template.module.example.api;

/**
 * 创建示例聚合的入参契约。
 */
public record ExampleCreateCommand(String code, String name) {
}
```

```java
package com.xingyun.template.module.example.api;

import com.xingyun.template.module.example.domain.model.ExampleId;

/**
 * 示例聚合的出参契约。
 */
public record ExampleResult(ExampleId id, String code, String name) {
}
```

```java
package com.xingyun.template.module.example.api;

import com.xingyun.template.module.example.domain.model.ExampleId;

import java.util.Optional;

/**
 * 示例模块对外契约：本模块全部对外能力的唯一视图（跨模块调用仅可依赖本包）。
 */
public interface ExampleApi {

    /**
     * 按标识查询示例聚合。
     *
     * @param id 聚合标识，不得为 {@code null}
     * @return 命中时返回出参契约；不存在时返回 {@code empty}
     */
    Optional<ExampleResult> findById(ExampleId id);

    /**
     * 创建并保存示例聚合。
     *
     * <p>{@code name} / {@code code} 不得为 {@code null}（编程契约，快速失败）。</p>
     *
     * @param command 创建入参
     * @return 携带主键的出参契约
     */
    ExampleResult create(ExampleCreateCommand command);
}
```

```java
package com.xingyun.template.module.example.application.service;

import com.xingyun.template.module.example.api.ExampleApi;
import com.xingyun.template.module.example.api.ExampleCreateCommand;
import com.xingyun.template.module.example.api.ExampleResult;
import com.xingyun.template.module.example.domain.model.ExampleId;
import com.xingyun.template.module.example.domain.model.ExampleModel;
import com.xingyun.template.module.example.domain.repository.ExampleRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * ExampleApi 的应用服务实现：编排用例流程，内聚契约 DTO ↔ 领域模型转换。
 */
@Service
public class ExampleApiImpl implements ExampleApi {

    private final ExampleRepository exampleRepository;

    public ExampleApiImpl(ExampleRepository exampleRepository) {
        this.exampleRepository = exampleRepository;
    }

    @Override
    public Optional<ExampleResult> findById(ExampleId id) {
        return exampleRepository.findById(id).map(ExampleApiImpl::toResult);
    }

    @Override
    public ExampleResult create(ExampleCreateCommand command) {
        ExampleModel model = ExampleModel.create(command.code(), command.name());
        return toResult(exampleRepository.save(model));
    }

    // 契约 DTO ↔ 领域模型转换内聚于实现类，单向小映射不为它增设独立转换器
    private static ExampleResult toResult(ExampleModel model) {
        return new ExampleResult(model.getId(), model.getCode(), model.getName());
    }
}
```

### 3.8 Web 层：HTTP 适配器 (`infrastructure/web/`)

Controller 只做协议转换与 HTTP 语义（状态码、Location），用例编排委托 `api` 契约；Request 携带 Bean Validation 注解止步于 Web 层（校验失败由框架返回 400；`spring-boot-starter-validation` 需显式引入，web starter 不传递校验实现），Response 以裸值出 HTTP 边界；两者经 `Assembler` 与契约 DTO 互转（见 AGENTS.md §3 表格）。

```java
package com.xingyun.template.module.example.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建示例聚合的 HTTP 入参：Bean Validation 注解止步于 Web 层，不向内传递。
 */
public record ExampleCreateRequest(
        @NotBlank(message = "code 不能为空")
        String code,
        @NotBlank(message = "name 不能为空")
        String name) {
}
```

```java
package com.xingyun.template.module.example.infrastructure.web.response;

/**
 * 示例聚合的 HTTP 出参：字段以裸值表达，领域类型不越过 HTTP 边界。
 */
public record ExampleResponse(Long id, String code, String name) {
}
```

```java
package com.xingyun.template.module.example.infrastructure.web.assembler;

import com.xingyun.template.module.example.api.ExampleCreateCommand;
import com.xingyun.template.module.example.api.ExampleResult;
import com.xingyun.template.module.example.infrastructure.web.request.ExampleCreateRequest;
import com.xingyun.template.module.example.infrastructure.web.response.ExampleResponse;
import org.springframework.stereotype.Component;

/**
 * HTTP 词汇（Request/Response）↔ 契约词汇（Command/Result）的转换器：只映射数据，不编排用例。
 */
@Component
public class ExampleAssembler {

    /**
     * Request 转入参契约：字段校验已由框架在入站时完成。
     */
    public ExampleCreateCommand toCommand(ExampleCreateRequest request) {
        return new ExampleCreateCommand(request.code(), request.name());
    }

    /**
     * 出参契约转 Response：强类型标识在此还原为裸值。
     */
    public ExampleResponse toResponse(ExampleResult result) {
        return new ExampleResponse(result.id().value(), result.code(), result.name());
    }
}
```

```java
package com.xingyun.template.module.example.infrastructure.web.controller;

import com.xingyun.template.module.example.api.ExampleApi;
import com.xingyun.template.module.example.api.ExampleCreateCommand;
import com.xingyun.template.module.example.api.ExampleResult;
import com.xingyun.template.module.example.domain.model.ExampleId;
import com.xingyun.template.module.example.infrastructure.web.assembler.ExampleAssembler;
import com.xingyun.template.module.example.infrastructure.web.request.ExampleCreateRequest;
import com.xingyun.template.module.example.infrastructure.web.response.ExampleResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * 示例模块的 HTTP 适配器：只做协议转换与编解码，用例编排委托 ExampleApi。
 */
@RestController
@RequestMapping("/examples")
public class ExampleController {

    private final ExampleApi exampleApi;
    private final ExampleAssembler exampleAssembler;

    public ExampleController(ExampleApi exampleApi, ExampleAssembler exampleAssembler) {
        this.exampleApi = exampleApi;
        this.exampleAssembler = exampleAssembler;
    }

    /**
     * 创建示例聚合：校验失败由框架返回 400，成功返回 201 并在 Location 指向新资源。
     */
    @PostMapping
    public ResponseEntity<ExampleResponse> create(@Valid @RequestBody ExampleCreateRequest request) {
        ExampleCreateCommand command = exampleAssembler.toCommand(request);
        ExampleResult result = exampleApi.create(command);
        return ResponseEntity.created(URI.create("/examples/" + result.id().value()))
                .body(exampleAssembler.toResponse(result));
    }

    /**
     * 按标识查询示例聚合：不存在返回 404。
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExampleResponse> findById(@PathVariable Long id) {
        return exampleApi.findById(new ExampleId(id))
                .map(exampleAssembler::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
```

---

## 附录：内嵌数据库（示例模块的开箱存储）

example 模块的仓储链路需要真实数据库才能跑通，但模板不预设使用者的数据库环境，也不应要求先安装、建库才能运行。模板内置了 **H2**——一个以纯 Java 实现、可随应用进程内嵌启动的轻量级数据库，并以**内存模式**运行：库表与数据只存活于应用进程内，启动时重建、停止时消失，零安装、零外部配置。它让 `mvn spring-boot:run` 之后示例接口立即可用，是示例的运行期与开发期设施，不是生产存储选型——接入真实数据库或移除示例时，清理步骤见本附录「移除」。

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
