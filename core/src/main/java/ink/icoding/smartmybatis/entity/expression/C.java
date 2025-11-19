package ink.icoding.smartmybatis.entity.expression;

/**
 * 比较符枚举
 * @author gsk
 */
public enum C {
    /**
     * 比较是否相同, 等同于 equals
     */
    EQ("=", "等于"),

    /**
     * 比较是否不相同, 等同于 notEquals
     */
    NE("<>", "不等于"),

    /**
     * 比较是否大于, 等同于 greaterThan
     */
    GT(">", "大于"),

    /**
     * 比较是否大于等于, 等同于 greaterThanOrEqualTo
     */
    GTE(">=", "大于等于"),

    /**
     * 比较是否小于, 等同于 lessThan
     */
    LT("<", "小于"),

    /**
     * 比较是否小于等于, 等同于 lessThanOrEqualTo
     */
    LTE("<=", "小于等于"),

    /**
     * 比较是否模糊匹配, 等同于 like
     */
    LIKE("LIKE", "模糊匹配"),

    /**
     * 比较是否不匹配, 等同于 notLike
     */
    NOT_LIKE("NOT LIKE", "不匹配"),

    /**
     * 比较是否包含于, 等同于 in
     */
    IN("IN", "包含于"),

    /**
     * 比较是否不包含于, 等同于 notIn
     */
    NOT_IN("NOT IN", "不包含于"),

    /**
     * 比较是否相同
     */
    equals("=", "等于"),

    /**
     * 比较是否不相同
     */
    notEquals("<>", "不等于"),

    /**
     * 比较是否大于
     */
    greaterThan(">", "大于"),

    /**
     * 比较是否大于等于
     */
    greaterThanOrEqualTo(">=", "大于等于"),

    /**
     * 比较是否小于
     */
    lessThan("<", "小于"),

    /**
     * 比较是否小于等于
     */
    lessThanOrEqualTo("<=", "小于等于"),

    /**
     * 比较是否模糊匹配
     */
    like("LIKE", "模糊匹配"),

    /**
     * 比较是否不匹配
     */
    notLike("NOT LIKE", "不匹配"),

    /**
     * 比较是否包含于
     */
    in("IN", "包含于"),

    /**
     * 比较是否不包含于
     */
    notIn("NOT IN", "不包含于");

    private final String value;
    private final String desc;

    C(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public String value() {
        return value;
    }

    public String desc() {
        return desc;
    }
}
