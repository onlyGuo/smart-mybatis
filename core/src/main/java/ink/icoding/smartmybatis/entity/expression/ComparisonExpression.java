package ink.icoding.smartmybatis.entity.expression;

import ink.icoding.smartmybatis.entity.po.PO;

import java.io.Serializable;

/**
 * 条件比较表类
 * @author gsk
 */
public class ComparisonExpression<T extends PO> implements Serializable {
    private SFunction<T, ?> func;
    private C comparison;
    private Object value;
    private Link link;

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
