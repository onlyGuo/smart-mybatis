package ink.icoding.smartmybatis.entity.expression;

import ink.icoding.smartmybatis.entity.po.PO;

import java.io.Serializable;

/**
 * 条件比较表类
 * @author gsk
 */
public class ComparisonExpression<T extends PO> implements Expression<T> {
    private SFunction<T, ?> func;
    private C comparison;
    private Object value;
    private Link link;

    /**
     * 构造函数
     * @param func 属性函数
     * @param comparison 比较符(当比较符为 LIKE 时, 且value前后没有 %, 则会自动添加 %. 若已包含 %, 则不添加)
     * @param value 比较值
     * @param link 连接符
     */
    public ComparisonExpression(SFunction<T, ?> func, C comparison, Object value, Link link) {
        this.func = func;
        this.comparison = comparison;
        this.value = value;
        this.link = link;
    }

    public SFunction<T, ?> getFunc() {
        return func;
    }

    public void setFunc(SFunction<T, ?> func) {
        this.func = func;
    }

    public C getComparison() {
        return comparison;
    }

    public void setComparison(C comparison) {
        this.comparison = comparison;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Link getLink() {
        return link;
    }

    public void setLink(Link link) {
        this.link = link;
    }
}
