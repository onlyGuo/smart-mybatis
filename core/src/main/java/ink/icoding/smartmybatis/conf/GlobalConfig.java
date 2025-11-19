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
     * 命名规范
     */
    private NamingConvention namingConvention = NamingConvention.UNDERLINE_UPPER;

    private String tablePrefix = "";

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

    @Override
    public String toString() {
        return "GlobalConfig{" +
                "enabled=" + enabled +
                ", autoSyncDb=" + autoSyncDb +
                ", namingConvention=" + namingConvention +
                ", tablePrefix='" + tablePrefix + '\'' +
                '}';
    }
}
