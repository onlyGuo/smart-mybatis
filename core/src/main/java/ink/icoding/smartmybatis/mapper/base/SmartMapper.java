package ink.icoding.smartmybatis.mapper.base;

import ink.icoding.smartmybatis.entity.Page;
import ink.icoding.smartmybatis.entity.PageResult;
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
     * 根据条件分页查询记录
     * @param where
     *      查询条件
     * @param page
     *      分页信息
     * @return 分页结果
     */
    default PageResult<T> selectPage(Where where, Page page){
        long total = count(where);
        List<T> list = select(where.limit((page.getPage() - 1) * page.getPageSize(), page.getPageSize()));
        return new PageResult<>(list, total, page.getPage(), page.getPageSize());
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

    /**
     * 执行 SQL 脚本文件中的 SQL 语句, 可能有多条, 要智能拆分后, 并开启事务, 然后通过 executeSql依次执行
     * @param script
     *      SQL 脚本内容
     * @return 受影响的记录数
     */
    default int executeSqlScript(String script) {
        if (script == null || script.trim().isEmpty()) {
            return 0;
        }
        int totalAffected = 0;
        StringBuilder sb = new StringBuilder();

        // 状态标记
        // '...'
        boolean inSingleQuote = false;
        // "..."
        boolean inDoubleQuote = false;
        // `...`
        boolean inBacktick = false;
        // /* ... */
        boolean inBlockComment = false;
        // -- ... or # ...
        boolean inLineComment = false;
        // 是否处于转义状态 (\后面)
        boolean isEscape = false;

        char[] chars = script.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            // 预读下一个字符（用于判断 -- 和 /* 和 */）
            char next = (i + 1 < chars.length) ? chars[i+1] : '\0';

            // 1. 处理转义 (仅在字符串或反引号内有效，或者普通模式下的转义)
            // MySQL 默认 \ 是转义符。
            if (isEscape) {
                sb.append(c);
                isEscape = false;
                continue;
            }

            // 2. 如果在单引号字符串内
            if (inSingleQuote) {
                sb.append(c);
                if (c == '\\') {
                    isEscape = true;
                } else if (c == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }

            // 3. 如果在双引号字符串内
            if (inDoubleQuote) {
                sb.append(c);
                if (c == '\\') {
                    isEscape = true;
                } else if (c == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }

            // 4. 如果在反引号内 (数据库表名/列名)
            if (inBacktick) {
                sb.append(c);
                // 反引号内通常不支持 \ 转义，而是用 double backtick `` 转义，但 MySQL 有时也支持 \
                // 这里按标准处理：遇到 ` 结束
                if (c == '`') {
                    inBacktick = false;
                }
                continue;
            }

            // 5. 如果在块注释内 /* ... */
            if (inBlockComment) {
                sb.append(c);
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    // 追加 /
                    sb.append('/');
                    // 跳过下一个字符
                    i++;
                }
                continue;
            }

            // 6. 如果在行注释内 -- ... 或 # ...
            if (inLineComment) {
                sb.append(c);
                if (c == '\n') {
                    inLineComment = false;
                }
                continue;
            }

            // --- 此时处于普通 SQL 模式 ---

            // 检查是否开始转义
            if (c == '\\') {
                isEscape = true;
                sb.append(c);
                continue;
            }

            // 检查单引号
            if (c == '\'') {
                inSingleQuote = true;
                sb.append(c);
                continue;
            }

            // 检查双引号
            if (c == '"') {
                inDoubleQuote = true;
                sb.append(c);
                continue;
            }

            // 检查反引号
            if (c == '`') {
                inBacktick = true;
                sb.append(c);
                continue;
            }

            // 检查 # 注释
            if (c == '#') {
                inLineComment = true;
                sb.append(c);
                continue;
            }

            // 检查 -- 注释
            if (c == '-' && next == '-') {
                inLineComment = true;
                sb.append(c);
                sb.append(next);
                i++;
                continue;
            }

            // 检查 /* 注释
            if (c == '/' && next == '*') {
                inBlockComment = true;
                sb.append(c);
                sb.append(next);
                i++; // 消耗 *
                continue;
            }

            // 检查分号 -> 拆分点
            if (c == ';') {
                String sql = sb.toString().trim();
                if (!sql.isEmpty()) {
                    totalAffected += executeSql(sql, new java.util.HashMap<>());
                }
                // 清空
                sb.setLength(0);
                continue;
            }

            // 普通字符
            sb.append(c);
        }

        // 处理最后剩余的 SQL
        String lastSql = sb.toString().trim();
        if (!lastSql.isEmpty()) {
            totalAffected += executeSql(lastSql, new java.util.HashMap<>());
        }

        return totalAffected;
    }
}
