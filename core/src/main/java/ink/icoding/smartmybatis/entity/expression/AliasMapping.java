package ink.icoding.smartmybatis.entity.expression;

import ink.icoding.smartmybatis.entity.po.PO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 字段别名映射
 * @author gsk
 */
public class AliasMapping <T extends PO> {
    private Class<T> entityClass;
    private String alias;
    // 目前只有LEFT JOIN
    private String type;
    // ON 条件
    private Where onWhere;

    private List<SFunction<? extends PO, ?>> selectFields;

    @SafeVarargs
    public AliasMapping(Class<T> entityClass, String alias, String type, SFunction<? extends PO, ?> ... args) {
        this.entityClass = entityClass;
        this.alias = alias;
        this.type = type;
        this.selectFields = new ArrayList<>();
        if (args != null) {
            this.selectFields.addAll(Arrays.asList(args));
        }
    }

    public String getAlias() {
        return alias;
    }

    /**
     * 指定别名表中的字段
     * @param field 字段
     * @return 字段别名映射
     */
    public FieldAliasMapping<T> col(SFunction<T, ?> field) {
        return new FieldAliasMapping<>(field, this.alias);
    }

    /**
     * 获取连接类型
     * @return 连接类型
     */
    public String getType() {
        return type;
    }

    public void on(Where onWhere) {
        this.onWhere = onWhere;
    }

    public Where getOnWhere() {
        return onWhere;
    }

    public Class<T> getEntityClass() {
        return entityClass;
    }

    public List<SFunction<? extends PO, ?>> getSelectFields() {
        return selectFields;
    }
}
