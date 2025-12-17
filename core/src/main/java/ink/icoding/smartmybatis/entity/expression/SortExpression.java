package ink.icoding.smartmybatis.entity.expression;

import ink.icoding.smartmybatis.entity.po.PO;

import java.io.Serializable;

/**
 * 排序表达式
 * @author gsk
 */
public class SortExpression<T extends PO> implements Serializable {
    private SFunction<T, ?> func;
    private SortDirection direction;

    /**
     * 构造函数
     * @param func 属性函数
     * @param direction 排序方向
     */
    public SortExpression(SFunction<T, ?> func, SortDirection direction) {
        this.func = func;
        this.direction = direction;
    }

    public SFunction<T, ?> getFunc() {
        return func;
    }

    public void setFunc(SFunction<T, ?> func) {
        this.func = func;
    }

    public SortDirection getDirection() {
        return direction;
    }

    public void setDirection(SortDirection direction) {
        this.direction = direction;
    }
}
