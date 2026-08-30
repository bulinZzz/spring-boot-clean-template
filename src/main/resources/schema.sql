-- 示例模块演示表：字段与 ExamplePO 一一对应
CREATE TABLE IF NOT EXISTS t_example (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL
);
