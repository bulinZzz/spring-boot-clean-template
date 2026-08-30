package com.xingyun.template.module.example.api;

import com.xingyun.template.module.example.domain.model.ExampleId;

import java.util.Optional;

/**
 * 示例模块对外契约：本模块全部对外能力的唯一视图（跨模块调用仅可依赖本包）。
 */
public interface ExampleApi {

    /**
     * 按标识查询示例聚合。
     *
     * @param id 聚合标识，不得为 {@code null}
     * @return 命中时返回出参契约；不存在时返回 {@code empty}
     */
    Optional<ExampleResult> findById(ExampleId id);

    /**
     * 创建并保存示例聚合。
     *
     * <p>{@code name} / {@code code} 不得为 {@code null}（编程契约，快速失败）。</p>
     *
     * @param command 创建入参
     * @return 携带主键的出参契约
     */
    ExampleResult create(ExampleCreateCommand command);
}
