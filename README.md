# spring-boot-clean-template

以整洁架构（Clean Architecture）与依赖倒置原则（DIP）为纲的 Spring Boot 基础模板：核心业务与 Web、数据库、第三方 SDK 等技术细节彻底解耦。

AI 生成的代码容易产出贫血模型与框架泄漏，人工开发的架构边界也会随工期失守。本模板把架构红线内置为自动加载的 AI 约束（`AGENTS.md`），让代码从第一行起就长在边界内。

**技术栈**：Java 17 · Spring Boot · MyBatis-Plus · Lombok · H2（内嵌示例库）· Maven · JUnit 5

## 适用人群

* **AI Coding 率超过 70% 的程序员** —— 人与 AI 共用同一套硬约束，AI 产出越多收益越大
* **重度代码洁癖者** —— 每个边界都有唯一判据，没有"大概没关系"
* **纯 CRUD 小项目不适用** —— 分层在此是过度设计，建议选用更简单的脚手架

## 快速开始

1. **模板实例化**：在 GitHub 页面点击 **Use this template** 创建新仓库。
2. **包名重构**：对根包 `com.xingyun.template` 执行全局重命名（`Shift + F6`），替换为实际项目包路径。
3. **启动验证**：执行 `mvn spring-boot:run`，服务基于内嵌 H2 数据库启动，无需安装或准备外部数据库。
4. **开发前准备**：建议先阅读 [ARCHITECTURE.md](./ARCHITECTURE.md) 与 [AGENTS.md](./AGENTS.md)，对架构边界建立整体认识。
5. **搭建模块**：项目提供 `module/example/`（完整分层）与 `module/comment/`（跨模块调用）两个范例，参照其包结构与代码搭建业务模块；范例与内嵌 H2 的介绍、使用及可优雅移除的步骤见 [docs/examples.md](./docs/examples.md)。

## 文档导航

| 文档 | 内容 | 读者 |
| :--- | :--- | :--- |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | 分层架构总则、包结构蓝图、可运行范例导引 | 所有开发者 |
| [AGENTS.md](./AGENTS.md) | AI 导航协议、架构红线、数据模型隔离、仓储与防腐、工程规则与代码生成约定 | 开发者与 AI Agent（自动加载） |
| [docs/examples.md](./docs/examples.md) | 范例模块（example/comment）与内嵌 H2 的介绍、使用与移除 | 参照或移除范例的开发者 |
