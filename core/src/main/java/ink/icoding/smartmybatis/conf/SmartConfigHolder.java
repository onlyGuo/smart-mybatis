package ink.icoding.smartmybatis.conf;

/**
 * 全局配置类
 * @author gsk
 */
public final class SmartConfigHolder {
    private static volatile GlobalConfig globalConfig;
    public static void init(GlobalConfig v) {
        globalConfig = v;
    }
    public static GlobalConfig config(){
        return globalConfig;
    }
}
