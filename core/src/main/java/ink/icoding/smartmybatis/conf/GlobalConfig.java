package ink.icoding.smartmybatis.conf;

/**
 * 全局配置类
 * @author gsk
 */
public class GlobalConfig {
    /**
     * 是否开启 Smart Mybatis 功能
     */
    private boolean enabled = true;

    /**
     * 是否根据实体类自动同步数据库表结构
     * 默认值：false, 开启本功能时, 只支持新增字段, 不支持修改和删除字段
     */
    private boolean autoSyncDb = false;

    /**
     * 是否保留 INPUT 策略由调用方显式提供的主键，默认 true。
     * 设为 false 时兼容 3.0.2 及之前将非空 INPUT 主键替换为 UUID 的行为。
     * 仅影响 INPUT；其他主键策略保持不变，INPUT 主键为空时始终拒绝插入。
     */
    private boolean preserveInputPrimaryKey = true;

    /**
     * 命名规范
     */
    private NamingConvention namingConvention = NamingConvention.UNDERLINE_UPPER;

    private String tablePrefix = "";

    /**
     * SQL 方言对应的 JDBC 驱动类名
     * <p>
     * 为空时自动从 spring.datasource.driver-class-name 读取并填充，
     * 然后根据驱动类名匹配对应的 SQL 方言实现。
     * <p>
     * 手动配置示例：com.mysql.cj.jdbc.Driver
     */
    private String dialectDriverClassName = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAutoSyncDb() {
        return autoSyncDb;
    }

    public void setAutoSyncDb(boolean autoSyncDb) {
        this.autoSyncDb = autoSyncDb;
    }

    public boolean isPreserveInputPrimaryKey() {
        return preserveInputPrimaryKey;
    }

    public void setPreserveInputPrimaryKey(boolean preserveInputPrimaryKey) {
        this.preserveInputPrimaryKey = preserveInputPrimaryKey;
    }

    public NamingConvention getNamingConvention() {
        return namingConvention;
    }

    public void setNamingConvention(NamingConvention namingConvention) {
        this.namingConvention = namingConvention;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public String getDialectDriverClassName() {
        return dialectDriverClassName;
    }

    public void setDialectDriverClassName(String dialectDriverClassName) {
        this.dialectDriverClassName = dialectDriverClassName;
    }

    @Override
    public String toString() {
        return "GlobalConfig{" +
                "enabled=" + enabled +
                ", autoSyncDb=" + autoSyncDb +
                ", preserveInputPrimaryKey=" + preserveInputPrimaryKey +
                ", namingConvention=" + namingConvention +
                ", tablePrefix='" + tablePrefix + '\'' +
                ", dialectDriverClassName='" + dialectDriverClassName + '\'' +
                '}';
    }
}
