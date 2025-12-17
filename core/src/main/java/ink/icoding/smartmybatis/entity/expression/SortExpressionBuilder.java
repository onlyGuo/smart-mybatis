package ink.icoding.smartmybatis.entity.expression;

import ink.icoding.smartmybatis.entity.po.PO;

/**
 * 排序表达式构建器
 * @author gsk
 */
public class SortExpressionBuilder <T extends PO>{
    private final Where where;
    private final SortExpression<T> sortExpression;

    /**
     * 构造函数
     * @param where Where 对象
     * @param sortExpression 排序表达式对象
     */
    public SortExpressionBuilder(Where where, SortExpression<T> sortExpression) {
        this.sortExpression = sortExpression;
        this.where = where;
    }

    /**
     * 设置排序方向为正序
     * @return SortExpression 对象
     */
    public Where asc() {
        sortExpression.setDirection(SortDirection.ASC);
        return where;
    }

    /**
     * 设置排序方向为倒序
     * @return SortExpression 对象
     */
    public Where desc() {
        sortExpression.setDirection(SortDirection.DESC);
        return where;
    }
}
