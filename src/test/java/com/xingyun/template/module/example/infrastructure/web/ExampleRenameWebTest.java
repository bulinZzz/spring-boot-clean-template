package com.xingyun.template.module.example.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xingyun.template.module.example.api.ExampleApi;
import com.xingyun.template.module.example.api.ExampleCreateCommand;
import com.xingyun.template.module.example.domain.model.ExampleId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 重命名用例的 Web 层验证：覆盖 204 成功、404 聚合不存在、409 聚合不变量违规（同名拒绝）
 * 三条状态路径——409 依赖全局异常翻译器，本测试同时是该翻译映射的回归保障。
 */
@SpringBootTest
class ExampleRenameWebTest {

    private final WebApplicationContext context;
    private final ExampleApi exampleApi;

    private MockMvc mockMvc;

    private ExampleId existingId;

    @Autowired
    ExampleRenameWebTest(WebApplicationContext context, ExampleApi exampleApi) {
        this.context = context;
        this.exampleApi = exampleApi;
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        existingId = exampleApi.create(new ExampleCreateCommand("E-RN", "旧名称")).id();
    }

    @Test
    @DisplayName("重命名为新名称返回 204")
    void rename_should_return_204_when_new_name() throws Exception {
        mockMvc.perform(put("/examples/{id}", existingId.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新名称\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("聚合不存在返回 404")
    void rename_should_return_404_when_aggregate_missing() throws Exception {
        mockMvc.perform(put("/examples/{id}", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新名称\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("同名拒绝返回 ProblemDetail 409")
    void rename_should_return_problem_detail_when_same_name() throws Exception {
        mockMvc.perform(put("/examples/{id}", existingId.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"旧名称\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Business Rule Violation"))
                .andExpect(jsonPath("$.detail").value("新名称与当前名称相同"));
    }
}
