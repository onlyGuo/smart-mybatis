package ink.icoding.smartmybatis.entity.expression;

import ink.icoding.smartmybatis.entity.po.PO;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * 查询条件表达式
 * @author gsk
 */
public class Where {

    private List<ComparisonExpression<?>> expressions;

    private int limitSize;

    private int limitStart;

    /**
     * 创建一个空的 Where 条件对象
     * @return Where 条件对象
     */
    public static Where where(){
        return new Where();
    }

    /**
     * 创建一个包含单个比较表达式的 Where 条件对象
     * @param func 属性函数
     * @param comparison 比较符
     * @param value 比较值
     * @return Where 条件对象
     */
    public static <T extends PO> Where where(SFunction<T, ?> func, C comparison, Object value){
        Where where = new Where();
        where.expressions = new LinkedList<>();
        where.expressions.add(new ComparisonExpression<>(func, comparison, value, null));
        return where;
    }

    /**
     * 获取比较表达式列表
     * @return 比较表达式列表
     */
    public List<ComparisonExpression<?>> getExpressions() {
        return expressions;
    }

    /**
     * 追加比较表达式
     * @param expression 比较表达式列表
     * @return 当前 Where 对象
     */
    public Where appendExpression(ComparisonExpression<?> expression) {
        if (this.expressions == null) {
            this.expressions = new LinkedList<>();
        }
        this.expressions.add(expression);
        return this;
    }

    /**
     * 添加一个 AND 连接的比较表达式
     * @param func 属性函数
     * @param comparison 比较符
     * @param value 比较值
     * @return 当前 Where 对象
     */
    public <T extends PO> Where and(SFunction<T, ?> func, C comparison, Object value) {
        return appendExpression(new ComparisonExpression<>(func, comparison, value, Link.AND));
    }

    /**
     * 当比较值不为空时, 则添加一个 AND 连接的比较表达式构建器
     * @param func 属性函数
     * @return 比较表达式构建器
     */
    public <T extends PO> Where ifAnd(SFunction<T, ?> func, C comparison, Object value) {
        if (null != value) {
            if (value instanceof String){
                if (((String) value).isEmpty()){
                    return this;
                }
            }
            return appendExpression(new ComparisonExpression<>(func, comparison, value, Link.AND));
        }
        return this;
    }

    /**
     * 添加一个 AND 连接的比较表达式构建器
     * @param func 属性函数
     * @return 比较表达式构建器
     */
    public <T extends PO> ComparisonExpressionBuilder<T> and(SFunction<T, ?> func) {
        return new ComparisonExpressionBuilder<T>(func, this, Link.AND, false);
    }

    /**
     * 添加一个 只有比较值不为空才生效的 AND 连接的比较表达式构建器
     * @param func 属性函数
     * @return 比较表达式构建器
     */
    public <T extends PO> ComparisonExpressionBuilder<T> ifAnd(SFunction<T, ?> func) {
        return new ComparisonExpressionBuilder<T>(func, this, Link.AND, true);
    }

    /**
     * 添加一个 OR 连接的比较表达式构建器
     * @param func 属性函数
     * @return 比较表达式构建器
     */
    public <T extends PO> ComparisonExpressionBuilder<T> or(SFunction<T, ?> func) {
        return new ComparisonExpressionBuilder<T>(func, this, Link.OR, false);
    }

    /**
     * 添加一个 只有比较值不为空才生效的 OR 连接的比较表达式构建器
     * @param func 属性函数
     * @return 比较表达式构建器
     */
    public <T extends PO> ComparisonExpressionBuilder<T> ifOr(SFunction<T, ?> func) {
        return new ComparisonExpressionBuilder<T>(func, this, Link.OR, true);
    }

    /**
     * 添加一个 OR 连接的比较表达式
     * @param func 属性函数
     * @param comparison 比较符
     * @param value 比较值
     * @return 当前 Where 对象
     */
    public <T extends PO> Where or(SFunction<T, ?> func, C comparison, Object value) {
        return appendExpression(new ComparisonExpression<>(func, comparison, value, Link.OR));
    }

    /**
     * 当比较值不为空时, 则添加一个 OR 连接的比较表达式构建器
     * @param func 属性函数
     * @return 比较表达式构建器
     */
    public <T extends PO> Where ifOr(SFunction<T, ?> func, C comparison, Object value) {
        if (null != value) {
            if (value instanceof String){
                if (((String) value).isEmpty()){
                    return this;
                }
            }
            return appendExpression(new ComparisonExpression<>(func, comparison, value, Link.OR));
        }
        return this;
    }


    /**
     * 获取限制记录数
     * @return 限制记录数
     */
    public Where limit(int length) {
        limitSize = length;
        return this;
    }

    /**
     * 获取限制记录数起始位置
     * @return 限制记录数起始位置
     */
    public Where limit(int start, int length){
        this.limitStart = start;
        this.limitSize = length;
        return this;
    }


    public int getLimitSize() {
        return limitSize;
    }


    public int getLimitStart() {
        return limitStart;
    }

}
