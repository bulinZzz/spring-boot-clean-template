# spring-boot-clean-template

以 **Clean Architecture、Hexagonal Architecture 与依赖倒置原则（DIP）** 为基础的 Spring Boot 项目模板。

模板将业务模型与 Web、数据库、第三方服务等技术细节隔离，并通过明确的模块边界与模型边界降低耦合。项目同时提供面向 AI Agent 的 `AGENTS.md`，使人工开发与 AI 辅助开发遵循同一套工程约束。

## 适用场景

适合需要长期维护、强调模块边界，并使用 AI 辅助编码的 Spring Boot 项目。

不适合以简单 CRUD 为主、架构边界带来的收益有限的小型项目。此类项目使用更简单的脚手架通常更合适。

## 技术栈

* Java 25
* Spring Boot 4.1.1
* MyBatis-Plus
* Lombok
* H2
* Maven
* JUnit 5
* ArchUnit

H2 仅作为模板示例的内存数据库，便于项目开箱运行，不代表生产环境的数据库选型。

## 快速开始

### 1. 创建项目

在 GitHub 中点击 **Use this template**，基于本仓库创建新的项目仓库。

### 2. 修改根包名

将示例根包 `com.xingyun.template` 重命名为实际项目包名，并同步调整对应目录。

全局搜索 `com.xingyun.template`，同步修改项目中的硬编码。

### 3. 启动项目

```bash
mvn spring-boot:run
```

模板内置 H2 内存数据库，不需要预先安装或配置外部数据库。

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

模板内置两个用于说明架构的示例模块：

* `module/example/`：完整分层示例，用于理解单个业务模块的结构、职责与典型写法。
* `module/comment/`：跨模块调用示例，用于理解模块公开面、契约依赖与模块间协作。

每个示例模块通常按以下路径阅读：

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

示例模块仅用于教学和架构演示，不应被视为模板使用者的实际业务模块。

## 文档导航

| 文档                                     | 内容                                | 适合谁                    |
| -------------------------------------- | --------------------------------- | ---------------------- |
| [ARCHITECTURE.md](./ARCHITECTURE.md)   | 架构总则、模块与包结构、依赖与边界、设计取舍        | 所有开发者                  |
| [AGENTS.md](./AGENTS.md)               | AI 工作规则、架构约束、工程规则、测试验证与编码约定       | AI Agent 与使用 AI 开发的开发者 |
| [docs/examples.md](./docs/examples.md) | 示例模块、H2 数据库的介绍、运行和移除 | 需要运行或清理模板示例的开发者     |
