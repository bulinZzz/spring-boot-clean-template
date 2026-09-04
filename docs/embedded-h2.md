# 内嵌数据库（示例模块的开箱存储）

示例模块（example 及消费它的 comment）的仓储链路需要真实数据库才能跑通，但模板不预设使用者的数据库环境，也不应要求先安装、建库才能运行。模板内置了 **H2**——一个以纯 Java 实现、可随应用进程内嵌启动的轻量级数据库，并以**内存模式**运行：库表与数据只存活于应用进程内，启动时重建、停止时消失，零安装、零外部配置。

H2 让 `mvn spring-boot:run` 之后示例接口立即可用，是示例的运行期与开发期设施，不是生产存储选型。接入真实数据库或移除示例时，清理步骤见本页「移除」。

> 架构蓝图见 [ARCHITECTURE.md](../ARCHITECTURE.md)。

## 构成

| 组成 | 位置 | 职责 |
| :--- | :--- | :--- |
| H2 驱动依赖 | `pom.xml`：`com.h2database:h2`（runtime 作用域） | 内嵌数据库的 JDBC 驱动 |
| 控制台依赖 | `pom.xml`：`org.springframework.boot:spring-boot-h2console` | H2 Web 控制台自动配置，缺它则 `spring.h2.console.enabled` 不生效 |
| 数据源配置 | `application.yml`：`spring.datasource` | 应用连接内存库的数据源；URL 中 `DB_CLOSE_DELAY=-1` 保证连接全部关闭后，库在应用运行期间不被销毁 |
| 控制台开关 | `application.yml`：`spring.h2.console.enabled` | 开启 H2 Web 控制台 |
| 建表脚本 | `src/main/resources/schema.sql` | `t_example`、`t_comment` 建表 DDL；位于 classpath 根目录，Spring Boot 对内嵌库默认在启动时自动执行 |

持久化框架 MyBatis-Plus（`mybatis-plus-spring-boot4-starter`）与具体数据库无关，不属于 H2 组成——更换数据库时它保留。

## 使用：查看内存库数据

1. 启动应用，浏览器访问 `http://localhost:8080/h2-console`；
2. 登录页连接信息与 `application.yml` 的 `spring.datasource` 配置一致：JDBC URL 填内存库地址 `jdbc:h2:mem:example`（控制台连接不带 `DB_CLOSE_DELAY` 参数），用户名与密码照配置填写；
3. 连接后即可查看示例与评论表（脚本中写作 `t_example`、`t_comment`，H2 对未加引号的标识符按大写存储，控制台中显示为 `T_EXAMPLE`、`T_COMMENT`），调用 `POST /examples`、`POST /comments` 写入的数据即时可见。

内存库只存活于应用进程内：IDEA 数据库工具、DBeaver 等外部客户端无法连接——在另一个 JVM 中用同一 URL 只会新建一个同名的空库；应用停止，数据即失。

## 移除

H2 仅服务于示例开箱即跑。接入真实数据库时按下列步骤替换；不需要持久化示例时，H2 构件按同一步骤删除：

1. **依赖**：删除 `pom.xml` 中的 `h2` 与 `spring-boot-h2console`；接入 MySQL 等数据库时替换为对应驱动。
2. **配置**：`application.yml` 中 `spring.datasource` 替换为真实数据库连接，删除 `spring.h2.console` 配置段。
3. **脚本**：删除 `src/main/resources/schema.sql`——它是 H2 方言 DDL；真实库建表交由迁移工具（如 Flyway、Liquibase）或 DBA 流程，主键需为数据库自增列以匹配 PO 的 `IdType.AUTO`。

若项目随之不再需要持久化，`mybatis-plus-spring-boot4-starter` 一并删除——类路径上存在持久化 starter 却无数据源配置时，应用启动失败。

---

**文档可移除**：按上述「移除」章节完成 H2 清理后，本文件即可一并删除。
