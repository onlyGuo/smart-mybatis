package ink.icoding.smartmybatis.entity.expression;

import ink.icoding.smartmybatis.entity.po.PO;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 查询条件表达式
 * @author gsk
 */
public class Where {

    private List<Expression<?>> expressions;

    private List<SortExpression<?>> sortExpressions;

    private Map<String, AliasMapping<?>> aliasMappings;

    private Map<Field, String> globalCacleAlias = new HashMap<>();

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
     * 将多个 Where 条件对象使用 OR 连接起来
     * @param where 第一个 Where 条件对象
     * @param wheres 其他 Where 条件对象
     * @return 连接后的 Where 条件对象
     */
    public static Where or(Where where, Where ... wheres){
        if (null == where){
            throw new IllegalArgumentException("The first 'where' parameter cannot be null.");
        }
        if (null == wheres || wheres.length == 0){
            throw new IllegalArgumentException("At least one 'where' parameter must be provided in the 'wheres' varargs.");
        }
        Where parent = Where.where();
        parent.appendExpression(new WhereExpression(where, Link.OR));
        for (Where w : wheres) {
            parent.appendExpression(new WhereExpression(w, Link.OR));
        }
        return parent;

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
     * 直接创建比较表达式构建器
     * @param func 属性函数
     * @return 比较表达式构建器
     */
    public static  <T extends PO> ComparisonExpressionBuilder<T> where(SFunction<T, ?> func) {
        Where where = new Where();
        return new ComparisonExpressionBuilder<T>(func, where, Link.AND, false);
    }

    /**
     * 直接创建比较表达式构建器(如果是空值则忽略)
     * @param func 属性函数
     * @return 比较表达式构建器
     */
    public static  <T extends PO> ComparisonExpressionBuilder<T> ifWhere(SFunction<T, ?> func) {
        Where where = new Where();
        return new ComparisonExpressionBuilder<T>(func, where, Link.AND, true);
    }


    /**
     * 获取比较表达式列表
     * @return 比较表达式列表
     */
    public List<Expression<?>> getExpressions() {
        return expressions;
    }

    /**
     * 追加比较表达式
     * @param expression 比较表达式列表
     * @return 当前 Where 对象
     */
    public Where appendExpression(Expression<?> expression) {
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
     * 添加一个 AND 连接的比较表达式
     * @param where 另一个 Where 条件对象
     * @return 当前 Where 对象
     */
    public Where and(Where where) {
        return appendExpression(new WhereExpression(where, Link.AND));
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

    /**
     * 添加排序表达式
     * @param func 属性函数
     * @param direction 排序方向
     * @return 当前 Where 对象
     */
    public <T extends PO> Where orderBy(SFunction<T, ?> func, SortDirection direction) {
        if (null == sortExpressions){
            sortExpressions = new LinkedList<>();
        }
        sortExpressions.add(new SortExpression<>(func, direction));
        return this;
    }

    /**
     * 重置排序表达式
     * @param func 属性函数
     * @param direction 排序方向
     * @return 当前 Where 对象
     */
    public <T extends PO> Where resetOrderBy(SFunction<T, ?> func, SortDirection direction) {
        sortExpressions = new LinkedList<>();
        sortExpressions.add(new SortExpression<>(func, direction));
        return this;
    }

    /**
     * 添加排序表达式构建器, 默认正序
     * @param func 属性函数
     * @return 排序表达式构建器
     */
    public <T extends PO> SortExpressionBuilder<T> orderBy(SFunction<T, ?> func) {
        SortExpression<T> tSortExpression = new SortExpression<>(func, SortDirection.ASC);
        if (null == sortExpressions){
            sortExpressions = new LinkedList<>();
        }
        sortExpressions.add(tSortExpression);
        return new SortExpressionBuilder<>(this, tSortExpression);
    }


    public List<SortExpression<?>> getSortExpressions() {
        return sortExpressions;
    }

    /**
     * 添加左关联查询
     * @param classType 别名对应的实体类
     * @param alias 别名
     * @param onWhere 连接条件
     * @param selectFields 选择的字段
     * @return 别名映射
     */
    @SafeVarargs
    public final <T extends PO, R extends PO> Where leftJoin(Class<T> classType, String alias, Where onWhere, SFunction<R, ?>... selectFields) {
        if (null == aliasMappings) {
            aliasMappings = new HashMap<>();
        }
        if (aliasMappings.containsKey(alias)) {
            throw new IllegalArgumentException("Alias '" + alias + "' is already used in the current query.");
        }
        AliasMapping<T> aliasMapping = new AliasMapping<>(classType, alias, "LEFT JOIN", selectFields);
        aliasMapping.on(onWhere);
        aliasMappings.put(alias, aliasMapping);
        return this;
    }

    public Map<String, AliasMapping<?>> getAliasMappings() {
        return aliasMappings;
    }

    public void setAliasMappings(Map<String, AliasMapping<?>> aliasMappings) {
        this.aliasMappings = aliasMappings;
    }

    public void putGlobalWhere(Field valueField, String valueAlias) {
        this.globalCacleAlias.put(valueField, valueAlias);
    }

    public String getGlobalWhereAliasValue(Field valueField) {
        return this.globalCacleAlias.get(valueField);
    }
}
