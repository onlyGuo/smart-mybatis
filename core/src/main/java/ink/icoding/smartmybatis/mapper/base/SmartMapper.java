package ink.icoding.smartmybatis.mapper.base;

import ink.icoding.smartmybatis.entity.SmartTreeNode;
import ink.icoding.smartmybatis.entity.expression.SFunction;
import ink.icoding.smartmybatis.entity.expression.Where;
import ink.icoding.smartmybatis.entity.po.PO;
import ink.icoding.smartmybatis.mapper.provider.BaseSqlProvider;
import ink.icoding.smartmybatis.utils.TreeUtils;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Smart Mybatis 基础 Mapper 接口
 * 所有 Mapper 接口需继承此接口以启用 Smart Mybatis 功能, 继承此接口后, 不影响原有的 Mybatis 功能
 * @author gsk
 */
public interface SmartMapper<T extends PO> {

    /**
     * 插入记录
     * @param record
     *      记录
     * @return 受影响的行数
     */
    @InsertProvider(type = BaseSqlProvider.class, method = "insert")
    int insert(@Param("record") T record);

    /**
     * 批量插入记录
     * @param records
     *      记录集合
     * @return 受影响的行数
     */
    @InsertProvider(type = BaseSqlProvider.class, method = "insertBatch")
    int insertBatch(@Param("list") Collection<T> records);

    /**
     * 查询所有记录
     * @return 记录列表
     */
    default List<T> selectAll(){
        return select(Where.where());
    }

    /**
     * 根据条件查询记录
     * @param where
     *      查询条件
     * @return 记录列表
     */
    @SelectProvider(type = BaseSqlProvider.class, method = "selectByWhere")
    List<T> select(Where where);

    /**
     * 根据条件查询树形结构数据
     * @param where
     *      查询条件
     * @param parentFunc
     *      父节点属性函数
     * @param idFunc
     *      节点ID属性函数
     * @return 树形结构数据列表
     */
    default List<SmartTreeNode<T, ?>> selectTrees(Where where, SFunction<T, ?> parentFunc, SFunction<T, ?> idFunc){
//        List<T> list = select(where);
//        return TreeUtils.buildTree(
//                list,
//                null, null,
//                null
//        );
        throw new RuntimeException("The method is not implemented yet.");
    }

    /**
     * 统计所有记录数
     * @return 记录数
     */
    long count();

    /**
     * 根据条件统计记录数
     * @param where
     *      查询条件
     * @return 记录数
     */
    @SelectProvider(type = BaseSqlProvider.class, method = "countByWhere")
    long count(Where where);

    /**
     * 根据主键查询记录
     * @param id
     *      主键
     * @return 记录
     */
    @SelectProvider(type = BaseSqlProvider.class, method = "selectByPrimaryKey")
    T selectById(Serializable id);

    /**
     * 根据条件查询单条记录, 如果有多条记录则抛出异常
     * @param where
     *      查询条件
     * @return 记录
     */
    @SelectProvider(type = BaseSqlProvider.class, method = "selectByWhere")
    T selectOne(Where where);

    /**
     * 根据条件查询第一条记录, 如果有多条记录则返回第一条
     * @param where
     *      查询条件
     * @return 记录
     */
    default T selectFirst(Where where){
        where.limit(1);
        List<T> list = select(where);
        if (list != null && !list.isEmpty()){
            return list.get(0);
        }
        return null;
    }


    /**
     * 根据自定义 SQL 语句查询记录
     * @param sql
     *      SQL 语句
     * @return 记录列表
     */
    default List<Map<String, Object>> queryBySql(String sql, Object... params){
        Map<String, Object> paramMap = new HashMap<>();
        for (int i = 0; i < params.length; i++) {
            paramMap.put("param" + i, params[i]);
        }
        for (int i = 0; i < params.length; i++) {
            sql = sql.replaceFirst("\\?", "#{params.param" + i + "}");
        }
        return queryBySql(sql, paramMap);
    }

    /**
     * 根据自定义 SQL 语句查询记录
     * @param sql
     *      SQL 语句
     * @param params
     *      参数
     * @return 记录列表
     */
    @SelectProvider(type = BaseSqlProvider.class, method = "queryBySql")
    List<Map<String, Object>> queryBySql(@Param("sql") String sql, @Param("params") Map<String, Object> params);

    /**
     * 根据主键删除记录
     * @param id
     *      主键
     * @return 受影响的行数
     */
    @UpdateProvider(type = BaseSqlProvider.class, method = "deleteById")
    int deleteById(Serializable id);

    /**
     * 根据主键集合删除记录
     * @param ids
     *      主键集合
     * @return 受影响的行数
     */
    @UpdateProvider(type = BaseSqlProvider.class, method = "deleteByIds")
    int deleteByIds(@Param("ids") Collection<? extends Serializable> ids);

    /**
     * 根据条件删除记录
     * @param where
     *      删除条件
     * @return 受影响的行数
     */
    @UpdateProvider(type = BaseSqlProvider.class, method = "deleteByWhere")
    int delete(Where where);

    /**
     * 根据主键更新记录
     * @param record
     *      记录
     * @return 受影响的行数
     */
    @UpdateProvider(type = BaseSqlProvider.class, method = "updateById")
    int updateById(@Param("record") T record);

    /**
     * 执行自定义 SQL 语句
     * @param sql
     *      SQL 语句
     * @param params
     *      参数
     * @return 受影响的行数
     */
    default int executeSql(String sql, Object... params){
        Map<String, Object> paramMap = new HashMap<>();
        for (int i = 0; i < params.length; i++) {
            paramMap.put("param" + i, params[i]);
        }
        for (int i = 0; i < params.length; i++) {
            sql = sql.replaceFirst("\\?", "#{params.param" + i + "}");
        }
        return executeSql(sql, paramMap);
    }

    /**
     * 执行自定义 SQL 语句
     * @param sql
     *      SQL 语句
     * @param params
     *      参数
     * @return 受影响的行数
     */
    @UpdateProvider(type = BaseSqlProvider.class, method = "executeSql")
    int executeSql(@Param("sql") String sql, @Param("params") Map<String, Object> params);
}
