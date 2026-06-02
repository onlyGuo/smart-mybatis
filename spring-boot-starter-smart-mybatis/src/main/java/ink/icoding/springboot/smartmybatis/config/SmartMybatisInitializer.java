package ink.icoding.springboot.smartmybatis.config;

import ink.icoding.smartmybatis.conf.SmartConfigHolder;
import ink.icoding.smartmybatis.mapper.provider.dialects.DialectResolver;
import ink.icoding.smartmybatis.mapper.provider.dialects.SqlDialects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Smart Mybatis 初始化器
 * @author gsk
 */
public class SmartMybatisInitializer implements
        org.springframework.context.ApplicationContextInitializer<org.springframework.context.ConfigurableApplicationContext> {

    private Logger logger = LoggerFactory.getLogger(SmartMybatisInitializer.class);

    @Override
    public void initialize(org.springframework.context.ConfigurableApplicationContext ctx) {

        ConfigurableEnvironment env = ctx.getEnvironment();

        // 使用 Binder 将配置绑定到 POJO（无需该类是 Bean）
        Binder binder = Binder.get(env);

        SmartMybatisProperties props = binder
                .bind("spring.mybatis.smart", Bindable.of(SmartMybatisProperties.class))
                .orElseGet(SmartMybatisProperties::new);

        // 如果用户未手动配置方言驱动类名，则从数据源驱动自动填充
        String driverClassName = props.getDialectDriverClassName();
        if (driverClassName == null || driverClassName.trim().isEmpty()) {
            driverClassName = env.getProperty("spring.datasource.driver-class-name");
            if (driverClassName != null && !driverClassName.trim().isEmpty()) {
                props.setDialectDriverClassName(driverClassName.trim());
            }
        }

        // 根据驱动类名解析方言
        SqlDialects dialect = DialectResolver.resolve(driverClassName);

        // 将配置和方言写入静态持有者，供 SqlProvider 使用
        SmartConfigHolder.init(props, dialect);

        logger.info("SmartMybatis initialized: {}, dialect: {}", props, dialect.getClass().getSimpleName());
    }
}
