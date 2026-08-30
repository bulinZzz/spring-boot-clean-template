package com.xingyun.template.module.example.domain.model;

import com.xingyun.template.module.example.domain.exception.ExampleRenameException;
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
     * 重命名：业务规则校验内聚于聚合根，失败抛领域业务异常（定义于 domain/exception/）。
     */
    public void rename(String newName) {
        if (name.equals(newName)) {
            throw new ExampleRenameException("新名称与当前名称相同");
        }
        this.name = newName;
    }
}
