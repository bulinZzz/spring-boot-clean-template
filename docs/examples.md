# 范例模块与运行时存储

模板内置两个可整体移除的教学模块（`example` 与消费其契约的 `comment`），以及支撑它们开箱即跑的内嵌 H2 数据库。三者均不含业务逻辑，不需要时按本文「移除范例」与「替换或移除 H2」章节清理。

> 架构蓝图见 [ARCHITECTURE.md](../ARCHITECTURE.md)；AI 仿写范例的行为规则见 [AGENTS.md §1.2](../AGENTS.md#1-ai-导航协议)。

## 范例模块

### example：完整分层教学件

`module/example/` 覆盖整洁架构全部分层，是新写某层代码时的仿写参照：

| 层 | 包路径 | 演示内容 |
| :--- | :--- | :--- |
| api | `module/example/api/` | 服务接口 + 契约 DTO（Command/Query/Result） |
| domain | `module/example/domain/` | 充血聚合根（`ExampleModel.rename` 演示不变量校验）、强类型 ID、仓储接口 |
| application | `module/example/application/service/` | 应用服务实现，内聚契约转换；`rename` 标注 `@Transactional` 演示事务边界 |
| infrastructure | `module/example/infrastructure/` | web（Controller/Request/Response/Assembler）、persistence（PO/Mapper/Converter/仓储实现） |

### comment：跨模块调用教学件

`module/comment/` 演示模块封装边界——仅依赖 `example` 的 `api` 包契约与契约签名引用的值语义类型（`ExampleId`），不触碰其内部实现；`example_id` 为逻辑关联，不建物理外键。

## H2：范例的运行时存储

范例的仓储链路需要真实数据库才能跑通，但模板不预设使用者的数据库环境。模板内置 **H2**——纯 Java 实现、可随应用进程内嵌启动的轻量级数据库，以**内存模式**运行：库表与数据只存活于应用进程内，启动时重建、停止时消失，零安装、零外部配置。

H2 让 `mvn spring-boot:run` 之后示例接口立即可用，是范例的运行期与开发期设施，不是生产存储选型。

### 构成

| 组成 | 位置 | 职责 |
| :--- | :--- | :--- |
| H2 驱动依赖 | `pom.xml`：`com.h2database:h2`（runtime 作用域） | 内嵌数据库的 JDBC 驱动 |
| 控制台依赖 | `pom.xml`：`org.springframework.boot:spring-boot-h2console` | H2 Web 控制台自动配置，缺它则 `spring.h2.console.enabled` 不生效 |
| 数据源配置 | `application.yml`：`spring.datasource` | 应用连接内存库的数据源；URL 中 `DB_CLOSE_DELAY=-1` 保证连接全部关闭后，库在应用运行期间不被销毁 |
| 控制台开关 | `application.yml`：`spring.h2.console.enabled` | 开启 H2 Web 控制台 |
| 建表脚本 | `src/main/resources/schema.sql` | 范例表的建表 DDL（`t_example`、`t_comment`）；位于 classpath 根目录，Spring Boot 对内嵌库默认在启动时自动执行 |

建表脚本归属范例，借 H2 自动执行机制落地——移除范例时一并删除；持久化框架 MyBatis-Plus 与具体数据库无关，更换数据库时保留。

### 使用：查看内存库数据

1. 启动应用，浏览器访问 `http://localhost:8080/h2-console`；
2. 登录页连接信息与 `application.yml` 的 `spring.datasource` 配置一致：JDBC URL 填内存库地址 `jdbc:h2:mem:example`（控制台连接不带 `DB_CLOSE_DELAY` 参数），用户名与密码照配置填写；
3. 连接后即可查看示例与评论表（脚本中写作 `t_example`、`t_comment`，H2 对未加引号的标识符按大写存储，控制台中显示为 `T_EXAMPLE`、`T_COMMENT`），调用 `POST /examples`、`POST /comments` 写入的数据即时可见。

内存库只存活于应用进程内：IDEA 数据库工具、DBeaver 等外部客户端无法连接——在另一个 JVM 中用同一 URL 只会新建一个同名的空库；应用停止，数据即失。

## 移除范例

范例不含业务逻辑，不需要时按下列步骤删除：

1. **源码**：删除 main 与 test 两个源码树下的 `module/example/` 与 `module/comment/` 目录。
2. **建表脚本**：删除 `src/main/resources/schema.sql`——它是范例的建表 DDL，范例移除后无表可建。
3. **本文件**：完成上述清理后，本文档即可一并删除。

若项目随之不再需要持久化，继续按下一节移除 H2 与 MyBatis-Plus。

## 替换或移除 H2

接入真实数据库时按下列步骤替换；不再需要持久化时，H2 与持久化框架按同一步骤删除：

1. **依赖**：删除 `pom.xml` 中的 `h2` 与 `spring-boot-h2console`；接入 MySQL 等数据库时替换为对应驱动。若不再需要持久化，`mybatis-plus-spring-boot4-starter` 一并删除。
2. **配置**：`application.yml` 中 `spring.datasource` 替换为真实数据库连接，删除 `spring.h2.console` 配置段；不再需要持久化时删除整个数据源配置段。
3. **脚本**：`schema.sql` 已随范例移除；真实库建表交由迁移工具（如 Flyway、Liquibase）或 DBA 流程，主键需为数据库自增列以匹配 PO 的 `IdType.AUTO`。

> 类路径上存在持久化 starter 却无数据源配置时，应用启动失败。
