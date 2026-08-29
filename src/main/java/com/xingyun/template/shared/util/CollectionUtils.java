package com.xingyun.template.shared.util;

import java.util.Collection;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 集合工具类 —— 全项目集合处理的唯一防腐层入口。
 *
 * <p><b>统一入口（Single Source）</b>：全项目禁止直接使用
 * {@code org.springframework.util.CollectionUtils}、{@code org.apache.commons.collections4.CollectionUtils}
 * 等任何第三方 {@code CollectionUtils}，所有集合工具调用必须经由本类完成；
 * 底层委托 Spring Framework 的 {@code CollectionUtils}（由 spring-core 提供，无额外依赖），
 * 可整体替换而调用方零改动。</p>
 *
 * <p><b>分层使用契约</b>（AGENTS.md §6 规则 2）：</p>
 * <ul>
 *   <li><b>外部数据边界层</b>（Controller / DTO / MQ Listener / RPC Client / Job 入参）：允许使用本类
 *       <b>任何提供 null 安全契约的方法</b>（判空等）处理<b>未校验的外部输入</b>。
 *       本类中标注 {@code @Nullable} 的参数代表"null 容忍"是明确契约，而非隐式吞没。</li>
 *   <li><b>内部核心业务层</b>（Service / Domain / Infrastructure）：禁用本类进行隐式 null 吞没——
 *       预期非 {@code null} 的集合必须使用
 *       {@link java.util.Objects#requireNonNull(Object, String)} 快速失败；
 *       已确认非 {@code null} 的集合必须使用 JDK 原生方法
 *       （如 {@code collection.isEmpty()}、{@code map.isEmpty()}）。</li>
 * </ul>
 *
 * <p>API 命名与语义以 Spring Framework 的 {@code CollectionUtils} 为基准，同名方法行为保持一致；
 * 方法集按需新增、不预先穷举封装，新增方法须同步声明 null 契约并附中文 Javadoc。
 * 本类声明 {@link NullMarked}：除显式标注 {@code @Nullable} 的参数与返回值外，
 * 其余参数与返回值均视为非 {@code null}。本类为无状态实现，线程安全。</p>
 *
 * @author bulinZzz
 */
@NullMarked
public final class CollectionUtils {

    private CollectionUtils() {
        throw new AssertionError("工具类禁止实例化");
    }

    // ------------------------------------------------------------------
    // 一、空值判断（边界层 null 容忍入口）
    // ------------------------------------------------------------------

    /**
     * 判断集合是否为 {@code null} 或不含任何元素。
     *
     * <p>{@code isEmpty(null)} / {@code isEmpty(List.of())} → {@code true}；
     * {@code isEmpty(List.of("a"))} → {@code false}。</p>
     *
     * @param collection 待检查的集合，可为 {@code null}
     * @return 为 {@code null} 或空集合时返回 {@code true}
     */
    public static boolean isEmpty(final @Nullable Collection<?> collection) {
        return org.springframework.util.CollectionUtils.isEmpty(collection);
    }

    /**
     * 判断集合是否非 {@code null} 且包含元素（{@link #isEmpty(Collection)} 的反义）。
     *
     * @param collection 待检查的集合，可为 {@code null}
     * @return 非 {@code null} 且包含元素时返回 {@code true}
     */
    public static boolean isNotEmpty(final @Nullable Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * 判断 Map 是否为 {@code null} 或不含任何键值对。
     *
     * <p>{@code isEmpty(null)} / {@code isEmpty(Map.of())} → {@code true}；
     * {@code isEmpty(Map.of("k", "v"))} → {@code false}。</p>
     *
     * @param map 待检查的 Map，可为 {@code null}
     * @return 为 {@code null} 或空 Map 时返回 {@code true}
     */
    public static boolean isEmpty(final @Nullable Map<?, ?> map) {
        return org.springframework.util.CollectionUtils.isEmpty(map);
    }

    /**
     * 判断 Map 是否非 {@code null} 且包含键值对（{@link #isEmpty(Map)} 的反义）。
     *
     * @param map 待检查的 Map，可为 {@code null}
     * @return 非 {@code null} 且包含键值对时返回 {@code true}
     */
    public static boolean isNotEmpty(final @Nullable Map<?, ?> map) {
        return !isEmpty(map);
    }
}
