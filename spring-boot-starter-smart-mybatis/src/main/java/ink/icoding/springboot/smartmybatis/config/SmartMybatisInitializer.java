package ink.icoding.springboot.smartmybatis.config;

import ink.icoding.smartmybatis.conf.SmartConfigHolder;
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

        // 将所需值写入静态持有者，供 SqlProvider 使用
        SmartConfigHolder.init(props);

        logger.info("SmartMybatis initialized: {}", props);
    }
}
