package com.xingyun.template.shared.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 字符串工具类 —— 全项目字符串处理的唯一防腐层入口。
 *
 * <p>全项目禁止直接使用任何第三方 {@code StringUtils}，字符串工具调用一律经由本类。
 * 底层委托 Apache Commons Lang3，可整体替换而调用方零改动。本类中 {@code @Nullable} 参数
 * 即边界层的 null 容忍契约入口——"null 容忍"是显式契约，而非隐式吞没；内部层调用须先经
 * {@code Objects.requireNonNull} 快速失败，其后直接使用 JDK 原生方法。</p>
 *
 * <p>方法集按需新增；当前判空族（isBlank / isNotBlank / isEmpty / isNotEmpty）作为统一入口的基线占位，
 * 其余能力按实际需求新增——新增方法委托底层库同名实现，同步声明 null 契约并附中文 Javadoc。
 * 本类声明 {@link NullMarked}——除显式标注 {@code @Nullable} 的参数与返回值外，其余均视为非 {@code null}；
 * 无状态实现，线程安全。</p>
 */
@NullMarked
public final class StringUtils {

    private StringUtils() {
        throw new AssertionError("工具类禁止实例化");
    }

    /**
     * 判断字符串是否为 {@code null}、空串或仅由空白字符组成。
     *
     * <p>{@code isBlank(null)} / {@code isBlank("")} / {@code isBlank(" ")} → {@code true}；
     * {@code isBlank("abc")} → {@code false}。</p>
     *
     * @param cs 待检查的字符串，可为 {@code null}
     * @return 为 {@code null}、空串或纯空白时返回 {@code true}
     */
    public static boolean isBlank(final @Nullable String cs) {
        return org.apache.commons.lang3.StringUtils.isBlank(cs);
    }

    /**
     * 判断字符串是否非空且包含非空白字符（{@link #isBlank(String)} 的反义）。
     *
     * @param cs 待检查的字符串，可为 {@code null}
     * @return 非 {@code null} 且包含非空白字符时返回 {@code true}
     */
    public static boolean isNotBlank(final @Nullable String cs) {
        return org.apache.commons.lang3.StringUtils.isNotBlank(cs);
    }

    /**
     * 判断字符串是否为 {@code null} 或空串（空白字符不算空）。
     *
     * <p>{@code isEmpty(null)} / {@code isEmpty("")} → {@code true}；
     * {@code isEmpty(" ")} → {@code false}。</p>
     *
     * @param cs 待检查的字符串，可为 {@code null}
     * @return 为 {@code null} 或空串时返回 {@code true}
     */
    public static boolean isEmpty(final @Nullable String cs) {
        return org.apache.commons.lang3.StringUtils.isEmpty(cs);
    }

    /**
     * 判断字符串是否非 {@code null} 且长度大于 0（{@link #isEmpty(String)} 的反义）。
     *
     * @param cs 待检查的字符串，可为 {@code null}
     * @return 非 {@code null} 且长度大于 0 时返回 {@code true}
     */
    public static boolean isNotEmpty(final @Nullable String cs) {
        return org.apache.commons.lang3.StringUtils.isNotEmpty(cs);
    }
}
