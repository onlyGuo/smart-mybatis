package ink.icoding.smartmybatis.entity.expression;

import ink.icoding.smartmybatis.entity.po.PO;

/**
 * 字段别名映射
 * @author gsk
 */
public class FieldAliasMapping <T extends PO>{
    private final SFunction<T, ?> field;
    private final String alias;

    public FieldAliasMapping(SFunction<T, ?> field, String alias) {
        this.field = field;
        this.alias = alias;
    }
    public SFunction<T, ?> getField() {
        return field;
    }
    public String getAlias() {
        return alias;
    }
}
