package ink.icoding.smartmybatis.utils.entity;

import ink.icoding.smartmybatis.entity.po.enums.TableField;
import ink.icoding.smartmybatis.utils.NamingUtil;

import java.lang.reflect.Field;

/**
 * 列声明信息
 * @author gsk
 */
public class ColumnDeclaration {

    private Field field;

    private String fieldName;

    private String columnName;

    private String columnType;

    private boolean json;

    private String description;

    private TableField annotation;

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public boolean isJson() {
        return json;
    }

    public void setJson(boolean json) {
        this.json = json;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TableField getAnnotation() {
        return annotation;
    }

    public void setAnnotation(TableField annotation) {
        this.annotation = annotation;
    }
}
