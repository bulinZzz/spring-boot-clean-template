package com.xingyun.template.module.example.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * t_example 表的物理表 PO，仅存在于基础设施持久化层，禁止外泄。
 */
@Getter
@Setter
@TableName("t_example")
public class ExamplePO {

    // 主键由数据库自增生成，insert 后由框架回填（不指定时 MyBatis-Plus 默认走雪花算法）
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
}
