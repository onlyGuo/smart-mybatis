package ink.icoding.springboot.smartmybatis.config;
import ink.icoding.smartmybatis.conf.GlobalConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Smart Mybatis 配置属性
 * @author gsk
 */
@ConfigurationProperties(prefix = "spring.mybatis.smart")
public class SmartMybatisProperties extends GlobalConfig {

}
