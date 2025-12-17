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
import org.springframework.beans.factory.config.BeanPostProcessor;
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
                    // 如果被baomidou代理了, 则转换为原生的MybatisMapper: com.baomidou.mybatisplus.core.override.MybatisMapperProxy
                    String string = bean.toString();
                    String baimidouClass = "com.baomidou.mybatisplus.core.override.MybatisMapperProxy";
                    if (string.contains(baimidouClass)){
                        // 发现是 MyBatis-Plus 的代理对象, 发出警告, 不建议混用 MyBatis-Plus 和 SmartMybatis,
                        // 因为MyBatis-Plus替换了MyBatis的核心代理实现, 这可能导致未知的兼容问题, 现已强行转换为原生的 MyBatis MapperProxy
                        LoggerFactory.getLogger(SmartMybatisAutoConfiguration.class)
                                .warn("The SmartMapper [{}] is proxied by MyBatis-Plus, which may cause compatibility issues. " +
                                        "It's recommended to use SmartMybatis without MyBatis-Plus. " +
                                        "Attempting to convert to native MyBatis MapperProxy.", mapperInterface.getName());
                        try {
                            // 1. 获取 MyBatis-Plus 的 InvocationHandler
                            java.lang.reflect.InvocationHandler mpHandler = java.lang.reflect.Proxy.getInvocationHandler(bean);
                            Class<?> mpHandlerClass = mpHandler.getClass();

                            // 2. 反射获取 sqlSession
                            java.lang.reflect.Field sqlSessionField = mpHandlerClass.getDeclaredField("sqlSession");
                            sqlSessionField.setAccessible(true);
                            org.apache.ibatis.session.SqlSession sqlSession = (org.apache.ibatis.session.SqlSession) sqlSessionField.get(mpHandler);

                            // 3. 反射获取 mapperInterface
                            java.lang.reflect.Field mapperInterfaceField = mpHandlerClass.getDeclaredField("mapperInterface");
                            mapperInterfaceField.setAccessible(true);
                            Class<?> originalMapperInterface = (Class<?>) mapperInterfaceField.get(mpHandler);

                            // 4. 反射获取 methodCache
                            //以此解决类型不兼容问题：java.util.Map<...MapperMethod> vs Map<...MapperMethodInvoker>
                            //同时避开 MapperMethodInvoker 可能是包级私有接口无法引用的问题
                            java.lang.reflect.Field methodCacheField = mpHandlerClass.getDeclaredField("methodCache");
                            methodCacheField.setAccessible(true);
                            java.util.Map methodCache = (java.util.Map) methodCacheField.get(mpHandler);

                            // 5. 创建原生的 MyBatis MapperProxy
                            // 使用 Raw Type 的 Map 传入构造函数，忽略泛型警告
                            @SuppressWarnings("unchecked")
                            org.apache.ibatis.binding.MapperProxy<Object> nativeHandler =
                                    new org.apache.ibatis.binding.MapperProxy<>(sqlSession, originalMapperInterface, methodCache);

                            // 6. 重新生成代理对象
                            bean = java.lang.reflect.Proxy.newProxyInstance(
                                    originalMapperInterface.getClassLoader(),
                                    new Class[]{originalMapperInterface, SmartMapper.class},
                                    nativeHandler
                            );

                            // 7. 更新外部的 mapperInterface 变量，确保日志打印正确
                            mapperInterface = originalMapperInterface;

                        } catch (Exception e) {
                            LoggerFactory.getLogger(SmartMybatisAutoConfiguration.class)
                                    .error("Failed to convert MybatisPlus proxy to native MapperProxy for bean: " + beanName, e);
                        }
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
