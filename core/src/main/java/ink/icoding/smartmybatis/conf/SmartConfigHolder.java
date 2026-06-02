package ink.icoding.smartmybatis.conf;

import ink.icoding.smartmybatis.mapper.provider.dialects.SqlDialects;
import ink.icoding.smartmybatis.mapper.provider.dialects.impl.MysqlDialect;

/**
 * 全局配置类
 * @author gsk
 */
public final class SmartConfigHolder {
    private static volatile GlobalConfig globalConfig;
    private static volatile SqlDialects dialect;

    /**
     * 初始化全局配置和方言
     *
     * @param v       全局配置
     * @param dialect SQL 方言实例
     */
    public static void init(GlobalConfig v, SqlDialects dialect) {
        globalConfig = v;
        if (dialect != null) {
            SmartConfigHolder.dialect = dialect;
        } else if (SmartConfigHolder.dialect == null) {
            SmartConfigHolder.dialect = new MysqlDialect();
        }
    }

    /**
     * 初始化全局配置（方言保持当前值或默认 MySQL）
     */
    public static void init(GlobalConfig v) {
        globalConfig = v;
        if (dialect == null) {
            dialect = new MysqlDialect();
        }
    }

    public static GlobalConfig config(){
        return globalConfig;
    }

    /**
     * 获取当前 SQL 方言
     * @return SQL 方言实例，默认为 MySQL 方言
     */
    public static SqlDialects getDialect() {
        if (dialect == null) {
            dialect = new MysqlDialect();
        }
        return dialect;
    }

    /**
     * 设置 SQL 方言
     * @param dialect SQL 方言实例
     */
    public static void setDialect(SqlDialects dialect) {
        SmartConfigHolder.dialect = dialect;
    }
}
