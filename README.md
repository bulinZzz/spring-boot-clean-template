# spring-boot-clean-template

以 **整洁架构（Clean Architecture）、六边形架构（Hexagonal Architecture）与依赖倒置原则（DIP）** 为基础的 Spring Boot 项目模板。

模板将业务模型与 Web、数据库、第三方服务等技术细节隔离，并通过明确的模块边界与模型边界降低耦合。项目同时提供面向 AI Agent 的 `AGENTS.md`，使人工开发与 AI 辅助开发遵循同一套工程约束。

## 适用场景

适合需要长期维护、强调模块边界，并使用 AI 辅助编码的 Spring Boot 项目。

不适合以简单 CRUD 为主、架构边界带来的收益有限的小型项目。此类项目使用更简单的脚手架通常更合适。

## 技术栈

* Java 25
* Spring Boot 4.1.1
* MyBatis-Plus
* Lombok
* H2（示例与开发用的内存库）
* Maven
* JUnit 5
* ArchUnit

## 快速开始

点击 **Use this template** 创建项目仓库，然后按 [派生指南](./template-docs/derivation.md) 完成从模板到项目的迁移。

## 文档导航

| 文档 | 内容 | 适合谁 |
| --- | --- | --- |
| [template-docs/ARCHITECTURE.md](./template-docs/ARCHITECTURE.md) | 模板的架构蓝图：架构总则、模块与包结构、依赖与边界、设计取舍 | 所有开发者 |
| [AGENTS.md](./AGENTS.md) | AI 工作规则、架构约束、工程规则、测试验证与编码约定 | AI Agent 与使用 AI 开发的开发者 |
| [template-docs/derivation.md](./template-docs/derivation.md) | 派生流程：项目标识迁移、上下文接管、目录引用、示例取舍、运行验证 | 初始化派生项目的开发者 |
