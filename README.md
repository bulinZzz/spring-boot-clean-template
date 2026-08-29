# spring-boot-clean-template

以整洁架构（Clean Architecture）与依赖倒置原则（DIP）为纲的 Spring Boot 基础模板：将核心业务与 Web、数据库、第三方 SDK 等技术细节彻底解耦。

AI 生成的代码天然倾向贫血模型与框架泄漏，人工开发的架构边界也会随工期失守——本模板把架构红线内置为自动加载的 AI 约束（`AGENTS.md`），让代码从第一行起就长在边界内。

## 🎯 适合谁

* **AI Coding 率超过 70% 的程序员** —— 人与 AI 共用同一套硬约束，AI 产出越多收益越大
* **重度代码洁癖者** —— 每个边界都有唯一判据，没有"大概没关系"
* **纯 CRUD 小项目不适用** —— 分层在此是过度设计，建议使用更简单的脚手架

依赖流向单向向内：`Infrastructure` ➔ `Application` ➔ `Domain`。

**技术栈**：Java 17 · Spring Boot · Maven · JUnit 5 · Lombok

## ⚡ 快速开始

1. **模板实例化**：在 GitHub 页面点击 **`Use this template`** 创建你的新仓库。
2. **包名重构**：用 IDE 打开项目，对根包 `com.xingyun.template` 执行全局重命名（`Shift + F6`），改为实际项目包路径。
3. **启动验证**：执行 `mvn spring-boot:run`，确认服务正常启动。
4. **开发前**：阅读下方两份文档。

## 📚 文档导航

| 文档 | 内容 | 读者 |
| :--- | :--- | :--- |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | 分层架构哲学、包结构蓝图、仓储模式标准代码 | 所有开发者 |
| [AGENTS.md](./AGENTS.md) | 架构红线、数据模型隔离、AI 代码生成偏好 | 开发者与 AI Agent（自动加载） |
