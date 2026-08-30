package com.xingyun.template.module.example.domain.model;

import com.xingyun.template.module.example.domain.exception.ExampleRenameException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ExampleModel 单元测试：聚合根创建与业务规则校验（纯领域，不依赖 Spring 容器）。
 */
class ExampleModelTest {

    @Test
    @DisplayName("create 快速失败 null 入参")
    void create_shouldFailFastOnNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> ExampleModel.create(null, "name"));
        assertThatNullPointerException()
                .isThrownBy(() -> ExampleModel.create("code", null));
    }

    @Test
    @DisplayName("create 产出的新聚合无主键")
    void create_shouldProduceTransientAggregate() {
        ExampleModel model = ExampleModel.create("code", "name");

        assertThat(model.getId()).isNull();
        assertThat(model.getName()).isEqualTo("name");
        assertThat(model.getCode()).isEqualTo("code");
    }

    @Test
    @DisplayName("reconstitute 携带主键重建聚合")
    void reconstitute_shouldCarryId() {
        ExampleModel model = ExampleModel.reconstitute(new ExampleId(1L), "code", "name");

        assertThat(model.getId()).isEqualTo(new ExampleId(1L));
    }

    @Test
    @DisplayName("rename 同名违反业务规则，抛领域业务异常")
    void rename_shouldRejectSameName() {
        ExampleModel model = ExampleModel.create("code", "name");

        assertThatThrownBy(() -> model.rename("name"))
                .isInstanceOf(ExampleRenameException.class)
                .hasMessage("新名称与当前名称相同");
    }

    @Test
    @DisplayName("rename 变更名称生效")
    void rename_shouldUpdateName() {
        ExampleModel model = ExampleModel.create("code", "name");

        model.rename("new-name");

        assertThat(model.getName()).isEqualTo("new-name");
    }
}
