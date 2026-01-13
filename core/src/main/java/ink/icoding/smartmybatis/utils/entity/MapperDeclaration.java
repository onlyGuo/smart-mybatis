package ink.icoding.smartmybatis.utils.entity;

import ink.icoding.smartmybatis.entity.po.PO;
import ink.icoding.smartmybatis.entity.po.enums.PrimaryGenerateType;
import ink.icoding.smartmybatis.entity.po.enums.TableField;

import java.io.Serializable;
import java.util.List;

/**
 * Mapper 声明信息
 * @author gsk
 */
public class MapperDeclaration {

    private Class<? extends PO> poClass;

    private String tableName;

    private Class<? extends Serializable> pkClass;

    private String pkName;

    private String pkColumnName;

    private PrimaryGenerateType pkGenerateType;

    private List<ColumnDeclaration> columnDeclarations;

    private TableField pkAnnotation;

    private String initScriptResourcePath;

    private String baseInsertSql;

    public Class<? extends PO> getPoClass() {
        return poClass;
    }

    public void setPoClass(Class<? extends PO> poClass) {
        this.poClass = poClass;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Class<? extends Serializable> getPkClass() {
        return pkClass;
    }

    public void setPkClass(Class<? extends Serializable> pkClass) {
        this.pkClass = pkClass;
    }

    public String getPkName() {
        return pkName;
    }

    public void setPkName(String pkName) {
        this.pkName = pkName;
    }

    public String getPkColumnName() {
        return pkColumnName;
    }

    public void setPkColumnName(String pkColumnName) {
        this.pkColumnName = pkColumnName;
    }

    public List<ColumnDeclaration> getColumnDeclarations() {
        return columnDeclarations;
    }

    public void setColumnDeclarations(List<ColumnDeclaration> columnDeclarations) {
        this.columnDeclarations = columnDeclarations;
    }

    public PrimaryGenerateType getPkGenerateType() {
        return pkGenerateType;
    }

    public void setPkGenerateType(PrimaryGenerateType pkGenerateType) {
        this.pkGenerateType = pkGenerateType;
    }

    public TableField getPkAnnotation() {
        return pkAnnotation;
    }

    public void setPkAnnotation(TableField pkAnnotation) {
        this.pkAnnotation = pkAnnotation;
    }

    public String getBaseInsertSql() {
        return baseInsertSql;
    }

    public void buildBaseSql(){
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append("`").append(getTableName()).append("`");
        sql.append(" (");
        StringBuilder valuesPart = new StringBuilder(" VALUES (");
        boolean first = true;
        List<ColumnDeclaration> columnDeclarations = getColumnDeclarations();
        for (ColumnDeclaration columnDeclaration : columnDeclarations) {
            if (!first) {
                sql.append(", ");
                valuesPart.append(", ");
            }
            sql.append("`").append(columnDeclaration.getColumnName()).append("`");
            if (columnDeclaration.isJson()){
                valuesPart.append("#{record.").append(columnDeclaration.getFieldName())
                        .append(", typeHandler=ink.icoding.smartmybatis.mapper.handlers.SmartJsonTypeHandler}");
            }else{
                valuesPart.append("#{record.").append(columnDeclaration.getFieldName()).append("}");
            }

            first = false;
        }

        if (getPkGenerateType() != PrimaryGenerateType.AUTO){
            if (!first) {
                sql.append(", ");
                valuesPart.append(", ");
            }
            sql.append("`").append(getPkColumnName()).append("`");
            valuesPart.append("#{record.").append(getPkName()).append("}");
        }
        sql.append(")");
        valuesPart.append(")");
        sql.append(valuesPart);
        baseInsertSql = sql.toString();
    }

    @Override
    public String toString() {
        return "MapperDeclaration{" +
                "poClass=" + poClass +
                ", tableName='" + tableName + '\'' +
                ", pkClass=" + pkClass +
                ", pkName='" + pkName + '\'' +
                ", pkColumnName='" + pkColumnName + '\'' +
                ", pkGenerateType=" + pkGenerateType +
                ", columnDeclarations=" + columnDeclarations +
                '}';
    }

    /**
     * 获取初始化脚本路径
     */
    public String getInitScriptResourcePath() {
        return initScriptResourcePath;
    }

    /**
     * 设置初始化脚本路径
     * @param initScriptResourcePath 初始化脚本路径
     */
    public void setInitScriptResourcePath(String initScriptResourcePath) {
        this.initScriptResourcePath = initScriptResourcePath;
    }
}
