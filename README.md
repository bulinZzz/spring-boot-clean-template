# spring-boot-clean-template

以 **Clean Architecture、Hexagonal Architecture 与依赖倒置原则（DIP）** 为基础的 Spring Boot 项目模板。

模板将业务模型与 Web、数据库、第三方服务等技术细节隔离，并通过明确的模块边界与模型边界降低耦合。项目同时提供面向 AI Agent 的 `AGENTS.md`，使人工开发与 AI 辅助开发遵循同一套工程约束。

## 适用场景

适合需要长期维护、强调模块边界，并大量使用 AI 辅助编码的 Spring Boot 项目。

不适合以简单 CRUD 为主、架构边界带来的收益有限的小型项目。此类项目使用更简单的脚手架通常更合适。

## 技术栈

* Java 17
* Spring Boot 4.1.1
* MyBatis-Plus
* Lombok
* H2
* Maven
* JUnit 5

H2 仅作为模板示例的内存数据库，便于项目开箱运行，不代表生产环境的数据库选型。

## 快速开始

### 1. 创建项目

在 GitHub 中点击 **Use this template**，基于本仓库创建新的项目仓库。

### 2. 修改根包名

将示例根包 `com.xingyun.template` 重命名为实际项目包名，并同步调整对应目录。

### 3. 启动项目

```bash
mvn spring-boot:run
```

模板内置 H2 内存数据库，不需要预先安装或配置外部数据库。示例模块启动后即可运行。

### 4. 验证项目

运行测试：

```bash
mvn test
```

执行完整构建：

```bash
mvn clean package
```

### 5. 开始开发

先阅读 [ARCHITECTURE.md](./ARCHITECTURE.md) 了解项目结构与边界，再阅读 `module/example/` 的完整分层实现。

一个业务模块通常按以下路径阅读：

```text
api
 ↓
application
 ↓
domain
 ↓
infrastructure
```

其中：

* `api` 定义模块对外公开的服务与契约。
* `application` 实现应用用例并负责用例编排。
* `domain` 承载业务模型与业务规则。
* `infrastructure` 负责 Web、数据库及外部系统等技术适配。

需要理解跨模块调用时，再阅读 `module/comment/`。

示例模块与 H2 的运行、替换和移除方式见 [docs/examples.md](./docs/examples.md)。

使用 AI 辅助开发时，再阅读 [AGENTS.md](./AGENTS.md)，其中包含 AI 的工作规则、架构约束、模型隔离、工程规则、测试与收尾要求。

## 文档导航

| 文档                                     | 内容                                       | 适合谁                    |
| -------------------------------------- | ---------------------------------------- | ---------------------- |
| [ARCHITECTURE.md](./ARCHITECTURE.md)   | 分层架构、模块与包结构、依赖边界、模型流转及设计取舍               | 所有开发者                  |
| [AGENTS.md](./AGENTS.md)               | AI 工作规则、架构约束、工程规则、测试验证与编码约定              | AI Agent 与使用 AI 开发的开发者 |
| [docs/examples.md](./docs/examples.md) | `example` / `comment` 范例，以及 H2 的使用、替换与移除 | 需要参考或清理模板范例的开发者        |

## 示例模块

模板内置两个用于说明架构的示例模块：

* `module/example/`：完整分层示例，用于理解各层职责与典型写法。
* `module/comment/`：跨模块调用示例，用于理解模块公开面与契约依赖。

示例可以整体删除，不应被视为模板使用者的业务模块。具体清理步骤见 [docs/examples.md](./docs/examples.md)。
