package com.xingyun.template.shared.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 字符串工具类 —— 全项目字符串处理的唯一防腐层入口。
 *
 * <p><b>统一入口（Single Source）</b>：全项目禁止直接使用
 * {@code org.apache.commons.lang3.StringUtils}、{@code org.springframework.util.StringUtils}
 * 等任何第三方 {@code StringUtils}，所有字符串工具调用必须经由本类完成；
 * 底层委托 Apache Commons Lang3，可整体替换而调用方零改动。</p>
 *
 * <p><b>分层使用契约</b>（AGENTS.md §6 规则 2）：</p>
 * <ul>
 *   <li><b>外部数据边界层</b>（Controller / DTO / MQ Listener / RPC Client / Job 入参）：允许使用本类
 *       <b>任何提供 null 安全契约的方法</b>（判空/修剪/兜底/包含/前后缀/截取/切分等）处理
 *       <b>未校验的外部输入</b>。本类中标注 {@code @Nullable} 的参数代表
 *       "null 容忍"是明确契约，而非隐式吞没。</li>
 *   <li><b>内部核心业务层</b>（Service / Domain / Infrastructure）：禁用本类进行隐式 null 吞没——
 *       预期非 {@code null} 的参数必须使用
 *       {@link java.util.Objects#requireNonNull(Object, String)} 快速失败；
 *       已确认非 {@code null} 的字符串必须使用 JDK 原生方法
 *       （如 {@code str.isBlank()}、{@code str.strip()}、{@code String.join()}）。</li>
 * </ul>
 *
 * <p>API 命名与语义以 Apache Commons Lang3 为基准，同名方法行为保持一致；
 * 方法集按需新增、不预先穷举封装，新增方法须同步声明 null 契约并附中文 Javadoc。
 * 本类声明 {@link NullMarked}：除显式标注 {@code @Nullable} 的参数与返回值外，
 * 其余参数与返回值均视为非 {@code null}。本类为无状态实现，线程安全。</p>
 *
 * @author bulinZzz
 */
@NullMarked
public final class StringUtils {

    private StringUtils() {
        throw new AssertionError("工具类禁止实例化");
    }

    // ------------------------------------------------------------------
    // 一、空值与空白判断（边界层 null 容忍入口）
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // 二、修剪归一化
    // ------------------------------------------------------------------

    /**
     * 去除字符串首尾的控制码点（{@code <= U+0020}，同 {@link String#trim()} 语义），结果为空时返回 {@code null}。
     *
     * <p>{@code trimToNull(null)} → {@code null}；{@code trimToNull("  ")} → {@code null}；
     * {@code trimToNull(" ab ")} → {@code "ab"}。</p>
     *
     * @param str 待修剪字符串，可为 {@code null}
     * @return 修剪后的字符串；空结果返回 {@code null}
     */
    public static @Nullable String trimToNull(final @Nullable String str) {
        return org.apache.commons.lang3.StringUtils.trimToNull(str);
    }

    /**
     * 去除字符串首尾的控制码点（{@code <= U+0020}，同 {@link String#trim()} 语义），结果为空时返回空串。
     *
     * <p>{@code trimToEmpty(null)} → {@code ""}；{@code trimToEmpty(" ab ")} → {@code "ab"}。</p>
     *
     * @param str 待修剪字符串，可为 {@code null}
     * @return 修剪后的字符串，永不为 {@code null}
     */
    public static String trimToEmpty(final @Nullable String str) {
        return org.apache.commons.lang3.StringUtils.trimToEmpty(str);
    }

    // ------------------------------------------------------------------
    // 三、默认值兜底
    // ------------------------------------------------------------------

    /**
     * 字符串为 {@code null} 时返回空串，否则原样返回。
     *
     * <p>{@code defaultString(null)} → {@code ""}；{@code defaultString("abc")} → {@code "abc"}。</p>
     *
     * @param str 待检查字符串，可为 {@code null}
     * @return 入参非 {@code null} 时原样返回，否则返回空串，永不为 {@code null}
     */
    public static String defaultString(final @Nullable String str) {
        return str == null ? "" : str;
    }

    /**
     * 字符串为 {@code null}、空串或纯空白时返回默认值，否则原样返回。
     *
     * <p>{@code defaultIfBlank("", "def")} → {@code "def"}；
     * {@code defaultIfBlank("abc", "def")} → {@code "abc"}；
     * {@code defaultIfBlank("   ", null)} → {@code null}。</p>
     *
     * @param str        待检查字符串，可为 {@code null}
     * @param defaultStr 默认值，可为 {@code null}
     * @return 非空白时原样返回（非 null），否则返回 {@code defaultStr}（可能为 null）
     */
    public static @Nullable String defaultIfBlank(final @Nullable String str, final @Nullable String defaultStr) {
        return isBlank(str) ? defaultStr : str;
    }

    /**
     * 字符串为 {@code null} 或空串时返回默认值，否则原样返回。
     *
     * <p>{@code defaultIfEmpty("", "def")} → {@code "def"}；
     * {@code defaultIfEmpty(" ", "def")} → {@code " "}；
     * {@code defaultIfEmpty(null, null)} → {@code null}。</p>
     *
     * @param str        待检查字符串，可为 {@code null}
     * @param defaultStr 默认值，可为 {@code null}
     * @return 非空时原样返回（非 null），否则返回 {@code defaultStr}（可能为 null）
     */
    public static @Nullable String defaultIfEmpty(final @Nullable String str, final @Nullable String defaultStr) {
        return isEmpty(str) ? defaultStr : str;
    }

    // ------------------------------------------------------------------
    // 四、相等比较
    // ------------------------------------------------------------------

    /**
     * 按内容比较两个字符串是否相等（null 安全）。
     *
     * <p>{@code equals(null, null)} → {@code true}；{@code equals(null, "abc")} → {@code false}；
     * {@code equals("abc", "abc")} → {@code true}。</p>
     *
     * @param cs1 比较项一，可为 {@code null}
     * @param cs2 比较项二，可为 {@code null}
     * @return 内容相等时返回 {@code true}
     */
    public static boolean equals(final @Nullable String cs1, final @Nullable String cs2) {
        return org.apache.commons.lang3.StringUtils.equals(cs1, cs2);
    }

    /**
     * 忽略大小写、按内容比较两个字符串是否相等（null 安全）。
     *
     * @param cs1 比较项一，可为 {@code null}
     * @param cs2 比较项二，可为 {@code null}
     * @return 忽略大小写后内容相等时返回 {@code true}
     */
    public static boolean equalsIgnoreCase(final @Nullable String cs1, final @Nullable String cs2) {
        return org.apache.commons.lang3.StringUtils.equalsIgnoreCase(cs1, cs2);
    }

    // ------------------------------------------------------------------
    // 五、包含与前后缀
    // ------------------------------------------------------------------

    /**
     * 判断字符串中是否包含给定子串（null 安全）。
     *
     * <p>{@code contains(null, "a")} / {@code contains("abc", null)} → {@code false}；
     * {@code contains("abcba", "cb")} → {@code true}；{@code contains("abc", "")} → {@code true}。</p>
     *
     * @param seq       待检查的字符串，可为 {@code null}
     * @param searchSeq 要搜索的子串，可为 {@code null}
     * @return 包含子串时返回 {@code true}；任意一者为 {@code null} 返回 {@code false}
     */
    public static boolean contains(final @Nullable String seq, final @Nullable String searchSeq) {
        return org.apache.commons.lang3.StringUtils.contains(seq, searchSeq);
    }

    /**
     * 判断字符串是否以指定前缀开头（null 安全，按字符匹配）。
     *
     * <p>{@code startsWith(null, "a")} / {@code startsWith("abc", null)} → {@code false}；
     * {@code startsWith("abcde", "ab")} → {@code true}；{@code startsWith("abc", "abcd")} → {@code false}；
     * 空前缀 {@code ""} 始终返回 {@code true}（非 null 入参）。</p>
     *
     * @param str    待检查的字符串，可为 {@code null}
     * @param prefix 前缀，可为 {@code null}
     * @return 以前缀开头时返回 {@code true}；任意一者为 {@code null} 返回 {@code false}
     */
    public static boolean startsWith(final @Nullable String str, final @Nullable String prefix) {
        return org.apache.commons.lang3.StringUtils.startsWith(str, prefix);
    }

    /**
     * 判断字符串是否以指定后缀结尾（null 安全，按字符匹配）。
     *
     * <p>{@code endsWith(null, "c")} / {@code endsWith("abc", null)} → {@code false}；
     * {@code endsWith("abcde", "de")} → {@code true}；{@code endsWith("abc", "xabc")} → {@code false}；
     * 空后缀 {@code ""} 始终返回 {@code true}（非 null 入参）。</p>
     *
     * @param str    待检查的字符串，可为 {@code null}
     * @param suffix 后缀，可为 {@code null}
     * @return 以后缀结尾时返回 {@code true}；任意一者为 {@code null} 返回 {@code false}
     */
    public static boolean endsWith(final @Nullable String str, final @Nullable String suffix) {
        return org.apache.commons.lang3.StringUtils.endsWith(str, suffix);
    }

    // ------------------------------------------------------------------
    // 六、截取
    // ------------------------------------------------------------------

    /**
     * 从指定起始位置截取子串（null 安全，越界容错）。
     *
     * <p>位置规则（同 Apache Commons Lang3）：正数从头部数、负数从尾部倒数（{@code -1} 为末位）；
     * 位置超出字符串长度时自动收敛，不会越界抛异常。
     * {@code substring(null, 0)} → {@code null}；{@code substring("abcde", 1)} → {@code "bcde"}；
     * {@code substring("abcde", -2)} → {@code "de"}；{@code substring("abc", 10)} → {@code ""}。</p>
     *
     * @param str   原字符串，可为 {@code null}
     * @param start 起始位置（含），负数从末尾倒数
     * @return 截取结果；入参为 {@code null} 时返回 {@code null}
     */
    public static @Nullable String substring(final @Nullable String str, final int start) {
        return org.apache.commons.lang3.StringUtils.substring(str, start);
    }

    /**
     * 从指定起止位置截取子串（null 安全，越界容错，起止顺序自动修正）。
     *
     * <p>位置规则：正数从头部数、负数从尾部倒数；位置超出长度自动收敛；
     * {@code start > end} 时自动交换后再截取，不会抛异常。
     * {@code substring(null, 0, 2)} → {@code null}；{@code substring("abcde", 1, 4)} → {@code "bcd"}；
     * {@code substring("abcde", 0, -1)} → {@code "abcd"}（末位排除）；{@code substring("abc", 4, 10)} → {@code ""}。</p>
     *
     * @param str   原字符串，可为 {@code null}
     * @param start 起始位置（含），负数从末尾倒数
     * @param end   结束位置（不含），负数从末尾倒数
     * @return 截取结果；入参为 {@code null} 时返回 {@code null}
     */
    public static @Nullable String substring(final @Nullable String str, final int start, final int end) {
        return org.apache.commons.lang3.StringUtils.substring(str, start, end);
    }

    // ------------------------------------------------------------------
    // 七、分割
    // ------------------------------------------------------------------

    /**
     * 按分隔字符组切分字符串（每个分隔字符独立生效，非正则；忽略相邻分隔符，不产出空元素）。
     *
     * <p>{@code split(null, ",")} → {@code null}；{@code split("a,b,  c", ", ")} → {@code ["a","b","c"]}；
     * {@code separatorChars} 为 {@code null} 时按空白切分。</p>
     *
     * @param str            原字符串，可为 {@code null}
     * @param separatorChars 分隔字符集合（其中每个字符都是分隔符），可为 {@code null}
     * @return 切分结果；入参为 {@code null} 时返回 {@code null}
     */
    public static String @Nullable [] split(final @Nullable String str, final @Nullable String separatorChars) {
        return org.apache.commons.lang3.StringUtils.split(str, separatorChars);
    }
}
