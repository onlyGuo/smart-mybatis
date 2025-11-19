package ink.icoding.smartmybatis.entity.expression;

import ink.icoding.smartmybatis.entity.po.PO;

/**
 * 比较表达式构建器
 * @author gsk
 */
public class ComparisonExpressionBuilder <T extends PO> {
    private final SFunction<T, ?> func;
    private final Where where;
    private final Link link;
    private final boolean ignoreNull;

    /**
     * 构造函数
     * @param func 属性函数
     * @param where Where 对象
     * @param link 连接符
     */
    public ComparisonExpressionBuilder(SFunction<T, ?> func, Where where, Link link, boolean ignoreNull) {
        this.func = func;
        this.where = where;
        this.link = link;
        this.ignoreNull = ignoreNull;
    }

    private boolean ifNull(Object value){
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).isEmpty();
        }
        return false;
    }

    private Where buildDefault(Object value, C comparison){
        if (ignoreNull) {
            if (ifNull(value)) {
                return where;
            }
        }
        return where.appendExpression(new ComparisonExpression<>(func, comparison, value, link));
    }

    /**
     * 与某值比较相同, 等同于 equals
     * @param value 比较值
     * @return Where 对象
     */
    public Where eq(Object value) {
        return buildDefault(value, C.EQ);
    }

    /**
     * 与某值比较相同
     * @param value 比较值
     * @return Where 对象
     */
    public Where equalsFor(Object value) {
        return buildDefault(value, C.EQ);
    }

    /**
     * 与某值比较不相同
     * @param value 比较值
     * @return Where 对象
     */
    public Where ne(Object value) {
        return buildDefault(value, C.NE);
    }
    /**
     * 与某值比较不相同
     * @param value 比较值
     * @return Where 对象
     */
    public Where notEquals(Object value) {
        return buildDefault(value, C.NE);
    }

    /**
     * 大于某值
     * @param value 比较值
     * @return Where 对象
     */
    public Where gt(Object value) {
        return buildDefault(value, C.GT);
    }

    /**
     * 大于某值
     * @param value 比较值
     * @return Where 对象
     */
    public Where greaterThan(Object value) {
        return buildDefault(value, C.GT);
    }

    /**
     * 大于等于某值
     * @param value 比较值
     * @return Where 对象
     */
    public Where gte(Object value) {
        return buildDefault(value, C.GTE);
    }

    /**
     * 大于等于某值
     * @param value 比较值
     * @return Where 对象
     */
    public Where greaterThanOrEquals(Object value) {
        return buildDefault(value, C.GTE);
    }

    /**
     * 小于某值
     * @param value 比较值
     * @return Where 对象
     */
    public Where lt(Object value) {
        return buildDefault(value, C.LT);
    }

    /**
     * 小于某值
     * @param value 比较值
     * @return Where 对象
     */
    public Where lessThan(Object value) {
        return buildDefault(value, C.LT);
    }

    /**
     * 小于等于某值
     * @param value 比较值
     * @return Where 对象
     */
    public Where lte(Object value) {
        return buildDefault(value, C.LTE);
    }

    /**
     * 小于等于某值
     * @param value 比较值
     * @return Where 对象
     */
    public Where lessThanOrEquals(Object value) {
        return buildDefault(value, C.LTE);
    }

    /**
     * 模糊匹配某值
     * @param value 比较值
     * @return Where 对象
     */
    public Where like(Object value) {
        return buildDefault(value, C.LIKE);
    }

    /**
     * 不匹配某值
     * @param value 比较值
     * @return Where 对象
     */
    public Where notLike(Object value) {
        return buildDefault(value, C.NOT_LIKE);
    }

    /**
     * 包含于某数组或集合中的元素
     * @param value 比较值
     * @return Where 对象
     */
    public Where in(Object value) {
        return buildDefault(value, C.IN);
    }

    /**
     * 不包含于某数组或集合中的元素
     * @param value 比较值
     * @return Where 对象
     */
    public Where notIn(Object value) {
        return buildDefault(value, C.NOT_IN);
    }
}
