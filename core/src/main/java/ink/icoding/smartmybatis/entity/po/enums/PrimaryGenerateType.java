package ink.icoding.smartmybatis.entity.po.enums;

/**
 * 主键生成类型
 * @author gsk
 */
public enum PrimaryGenerateType {
    /**
     * 自动增长(由数据库控制)
     */
    AUTO,
    /**
     * 用户自定义
     */
    UUID,
    /**
     * 雪花算法
     */
    SNOWFLAKE,
    /**
     * 16进制雪花算法
     */
    SNOWFLAKE_HEX,
    /**
     * 手动输入
     */
    INPUT
}
