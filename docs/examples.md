# 示例模块与运行时存储

模板内置两个可整体移除的教学模块——`example` 与消费其公开契约的 `comment`，以及支撑它们开箱即跑的内嵌 H2 数据库。

示例用于演示架构边界与各层典型写法，不承载模板使用者的实际业务；正式开发后可按本文末的说明整体清理。

## 示例模块

`module/example/` 覆盖 `api / domain / application / infrastructure` 四层，是完整分层示例。新写某层代码时，阅读该模块对应层的代码仿写即可，文件内的中文注释说明该层的写法要点。

`module/comment/` 演示模块之间如何通过公开契约协作：跨模块只依赖对方的 `api` 包，不访问对方的内部实现与数据表；跨模块引用仅作逻辑关联，不建立物理外键。

示例配套的测试同样按验证责任组织，可作为测试写法的参照：领域单元测试、Web 契约测试，以及位于测试根目录的 `ArchitectureTest`——架构边界的可执行守卫，不引用具体示例模块，移除示例后继续生效。

## 运行与试用

启动应用（H2 内存库随应用启动，无需预装数据库）：

```bash
mvn spring-boot:run
```

以下调用依次走通两个模块的主要路径（全新启动的内存库中，首个示例的标识为 1）：

```bash
# 创建示例
curl -X POST localhost:8080/examples \
  -H 'Content-Type: application/json' \
  -d '{"code":"DEMO","name":"第一个示例"}'

# 查询示例
curl localhost:8080/examples/1

# 重命名示例
curl -X PUT localhost:8080/examples/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"新名称"}'

# 为示例创建评论
curl -X POST localhost:8080/comments \
  -H 'Content-Type: application/json' \
  -d '{"exampleId":1,"content":"一条评论"}'

# 按示例列出评论
curl 'localhost:8080/comments?exampleId=1'
```

### 查看内存库数据

H2 Web 控制台默认关闭。将 `application.yml` 中的 `spring.h2.console.enabled` 改为 `true` 并重启，访问：

```text
http://localhost:8080/h2-console
```

登录页 JDBC URL 填写 `jdbc:h2:mem:example`，用户名与密码与 `spring.datasource` 配置一致（默认 `sa` / 空密码）。连接后可查看 `T_EXAMPLE`、`T_COMMENT` 两张表，接口调用产生的数据即时可见。

## H2 运行时存储

示例的仓储链路需要真实数据库才能跑通，而模板不预设使用者的数据库环境，因此内置 H2 并以内存模式运行：应用启动时建库，停止后数据消失，无需安装与配置。H2 是示例的运行期与开发期设施，不代表生产环境的数据库选型。

其构成如下：

- 依赖：`com.h2database:h2`（runtime 作用域的 JDBC 驱动）与 `org.springframework.boot:spring-boot-h2console`（H2 控制台自动配置；Spring Boot 4.x 中已独立为单独模块，缺它则控制台开关不生效）。
- 数据源：`application.yml` 的 `spring.datasource` 指向内存库；URL 中的 `DB_CLOSE_DELAY=-1` 保证全部连接关闭后，库在应用运行期间不被销毁。
- 建表脚本：`src/main/resources/schema.sql` 定义 `t_example`、`t_comment` 两张表；位于 classpath 根目录，Spring Boot 对内嵌库在启动时自动执行。

内存模式下数据只存在于当前应用进程：重启后数据重建，停止后消失；IDEA、DBeaver 等外部工具无法连接应用进程内的内存库，在另一个 JVM 中使用相同 JDBC URL 只会得到另一个独立的同名内存库。因此不应将它当作开发期的长期数据存储。

## 移除示例

示例不属于模板使用者的业务代码，不需要时可整体删除：

1. 删除两个模块的主代码与测试代码：`src/main/java` 与 `src/test/java` 下的 `module/example/`、`module/comment/` 目录（实际路径以当前根包为准）。
2. 删除 `src/main/resources/schema.sql`——该脚本只服务示例的两张表。
3. 删除本文档；若项目仍保留 H2 作为开发设施，可保留并裁剪 H2 相关章节。

`ArchitectureTest` 与 `shared/` 下的通用设施不引用具体示例模块，无需改动。清理后执行 `mvn test` 确认构建通过。

## 替换或移除 H2

项目接入真实数据库时：

1. 替换依赖：移除 `com.h2database:h2` 与 `spring-boot-h2console`，加入实际数据库的 JDBC 驱动；若同时不再使用 MyBatis-Plus，一并移除 `mybatis-plus-spring-boot4-starter`。
2. 替换配置：将 `application.yml` 中的 `spring.datasource` 改为实际数据库连接，并删除 `spring.h2.console` 配置；项目不再需要持久化时删除整个数据源配置。
3. 接管建表：生产环境的库结构应交由 Flyway、Liquibase 或 DBA 发布流程管理，不再依赖 `schema.sql` 自动建表。
4. 主键策略：示例 PO 使用 `IdType.AUTO`（数据库自增主键）；沿用这一映射时，真实数据库的主键需采用对应的自增策略。

> 类路径上存在持久化 starter 却没有可用数据源配置时，应用可能无法正常启动。
