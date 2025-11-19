package ink.icoding.springboot.smartmybatis.config;

import ink.icoding.smartmybatis.SpringApplicationUtil;
import ink.icoding.smartmybatis.conf.SmartConfigHolder;
import ink.icoding.smartmybatis.entity.po.PO;
import ink.icoding.smartmybatis.mapper.base.SmartMapper;
import ink.icoding.smartmybatis.mapper.interfaces.DefaultSmartMapperInitializer;
import ink.icoding.smartmybatis.mapper.interfaces.SmartMapperInitializer;
import jakarta.annotation.Resource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Smart Mybatis 自动配置类
 * @author gsk
 */
@AutoConfiguration
@EnableConfigurationProperties(SmartMybatisProperties.class)
public class SmartMybatisAutoConfiguration {

    @Resource
    private ApplicationContext applicationContext;

    private SmartMapperInitializer thisSmartMapperInitializer;

    @Bean
    public SpringApplicationUtil springApplicationUtil() {
        return new SpringApplicationUtil();
    }

    @Bean
    public SmartMybatisBootstrap smartBootstrap(SmartMybatisProperties props) {
        return new SmartMybatisBootstrap();
    }

    @Bean
    public BeanPostProcessor beanPostProcessor() {
        return new BeanPostProcessor() {

            /**
             * 初始化后处理器, 目前用于 SmartMapper 的注册的后续处理
             * @param bean
             *      Bean 实例
             * @param beanName
             *      Bean 名称
             * @return 处理后的 Bean 实例
             */
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (!SmartConfigHolder.config().isEnabled()){
                    return bean;
                }
                if (bean instanceof SmartMapper) {
                    Class<?> mapperInterface = bean.getClass().getInterfaces()[0];
                    SmartMapperInitializer smartMapperInitializer = null;
                    try {
                        smartMapperInitializer = applicationContext.getBean(SmartMapperInitializer.class);
                    } catch (BeansException e) {
                        // 如果没有自定义的 SmartMapperInitializer, 则使用默认的
                        if (thisSmartMapperInitializer == null){
                            thisSmartMapperInitializer = new DefaultSmartMapperInitializer(applicationContext);
                        }
                        smartMapperInitializer = thisSmartMapperInitializer;
                    }
                    smartMapperInitializer.initMapper((SmartMapper<? extends PO>)bean);
                    LoggerFactory.getLogger(SmartMybatisAutoConfiguration.class)
                            .info("Register SmartMapper: {}", mapperInterface.getName());
                }
                return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
            }
        };
    }

}
