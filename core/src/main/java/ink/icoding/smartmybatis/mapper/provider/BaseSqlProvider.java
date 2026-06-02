package ink.icoding.smartmybatis.mapper.provider;

import ink.icoding.smartmybatis.conf.SmartConfigHolder;
import ink.icoding.smartmybatis.entity.expression.Where;
import ink.icoding.smartmybatis.entity.po.PO;
import ink.icoding.smartmybatis.entity.po.enums.PrimaryGenerateType;
import ink.icoding.smartmybatis.mapper.provider.dialects.SqlDialects;
import ink.icoding.smartmybatis.utils.SnowflakeIdGeneratorUtil;
import ink.icoding.smartmybatis.utils.entity.MapperDeclaration;
import ink.icoding.smartmybatis.utils.entity.MapperUtil;
import org.apache.ibatis.builder.annotation.ProviderContext;

import java.io.Serializable;
import java.util.*;

/**
 * 基础 SQL 提供者
 * <p>
 * 负责元数据解析和主键生成，所有 SQL 生成委托给 {@link SqlDialects} 方言接口。
 *
 * @author gsk
 */
public class BaseSqlProvider {

    private static SqlDialects dialect() {
        return SmartConfigHolder.getDialect();
    }

    /**
     * 插入记录 SQL 语句生成
     */
    public <T extends PO> String insert(T record, ProviderContext context) {
        MapperDeclaration decl = MapperUtil.getMapperDeclaration(context.getMapperType());
        generatePk(record, decl);
        return dialect().buildInsert(decl);
    }

    /**
     * 批量插入记录 SQL 语句生成
     */
    public String insertBatch(Map<String, Object> params, ProviderContext context) {
        @SuppressWarnings("unchecked")
        Collection<PO> records = (Collection<PO>) params.get("list");
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("The records collection for batch insert cannot be null or empty.");
        }
        MapperDeclaration decl = MapperUtil.getMapperDeclaration(context.getMapperType());
        for (PO record : records) {
            generatePk(record, decl);
        }
        return dialect().buildInsertBatch(decl, records.size());
    }

    /**
     * 根据 Where 条件生成查询 SQL 语句
     */
    public String selectByWhere(Where where, ProviderContext context) {
        MapperDeclaration decl = MapperUtil.getMapperDeclaration(context.getMapperType());
        String whereSql = dialect().buildWherePart(where, false, "");
        return dialect().buildSelect(decl, where, whereSql, false);
    }

    /**
     * 根据 Where 条件生成查询 SQL 语句（含关联字段）
     */
    public String selectWithRelationsByWhere(Where where, ProviderContext context) {
        MapperDeclaration decl = MapperUtil.getMapperDeclaration(context.getMapperType());
        String whereSql = dialect().buildWherePart(where, false, "");
        return dialect().buildSelect(decl, where, whereSql, true);
    }

    /**
     * 根据 Where 条件生成统计记录数 SQL 语句
     */
    public String countByWhere(Where where, ProviderContext context) {
        MapperDeclaration decl = MapperUtil.getMapperDeclaration(context.getMapperType());
        String whereSql = dialect().buildWherePart(where, false, "");
        return dialect().buildCount(decl, whereSql);
    }

    /**
     * 根据主键生成查询 SQL 语句
     */
    public String selectByPrimaryKey(Serializable id, ProviderContext context) {
        MapperDeclaration decl = MapperUtil.getMapperDeclaration(context.getMapperType());
        return dialect().buildSelectById(decl);
    }

    /**
     * 自定义 SQL 查询
     */
    public String queryBySql(Map<String, Object> params, ProviderContext context) {
        return params.get("sql").toString();
    }

    /**
     * 自定义 SQL 执行
     */
    public String executeSql(Map<String, Object> params, ProviderContext context) {
        return params.get("sql").toString();
    }

    /**
     * 根据主键生成删除 SQL 语句
     */
    public String deleteById(Serializable id, ProviderContext context) {
        MapperDeclaration decl = MapperUtil.getMapperDeclaration(context.getMapperType());
        return dialect().buildDeleteById(decl);
    }

    /**
     * 根据主键集合生成批量删除 SQL 语句
     */
    public String deleteByIds(Collection<Serializable> ids, ProviderContext context) {
        MapperDeclaration decl = MapperUtil.getMapperDeclaration(context.getMapperType());
        return dialect().buildDeleteByIds(decl, ids.size());
    }

    /**
     * 根据 Where 条件生成删除 SQL 语句
     */
    public String deleteByWhere(Where where, ProviderContext context) {
        MapperDeclaration decl = MapperUtil.getMapperDeclaration(context.getMapperType());
        String whereSql = dialect().buildWherePart(where, false, "");
        return dialect().buildDelete(decl, whereSql);
    }

    /**
     * 根据主键生成更新 SQL 语句
     */
    public String updateById(Map<String, Object> params, ProviderContext context) {
        MapperDeclaration decl = MapperUtil.getMapperDeclaration(context.getMapperType());
        return dialect().buildUpdate(decl);
    }

    /**
     * 生成主键（UUID / SNOWFLAKE 等非自增类型）
     */
    @SuppressWarnings("unchecked")
    private <T extends PO> void generatePk(T record, MapperDeclaration decl) {
        if (decl.getPkGenerateType() == PrimaryGenerateType.AUTO) {
            return;
        }
        switch (decl.getPkGenerateType()) {
            case INPUT:
                Object pkValue = MapperUtil.getFieldValue(record, decl.getPkName());
                if (null == pkValue) {
                    throw new IllegalArgumentException(
                            "Primary key value must be provided for INPUT generate type, but it is null. at "
                                    + decl.getPoClass().getName());
                }
                // fall through: INPUT 不设置值，但保留原有 fall-through 行为
            case UUID:
                MapperUtil.setFieldValue(record, decl.getPkName(),
                        UUID.randomUUID().toString().replace("-", ""));
                break;
            case SNOWFLAKE:
                MapperUtil.setFieldValue(record, decl.getPkName(),
                        String.valueOf(SnowflakeIdGeneratorUtil.getInstance().nextId()));
                break;
            case SNOWFLAKE_HEX:
                MapperUtil.setFieldValue(record, decl.getPkName(),
                        Long.toHexString(SnowflakeIdGeneratorUtil.getInstance().nextId()));
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported primary key generate type: " + decl.getPkGenerateType()
                                + " at " + decl.getPoClass().getName());
        }
    }
}
