package ink.icoding.springboot.smartmybatis.config;

import java.util.Collections;

import junit.framework.TestCase;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;

import ink.icoding.smartmybatis.conf.GlobalConfig;
import ink.icoding.smartmybatis.conf.SmartConfigHolder;
import ink.icoding.smartmybatis.mapper.provider.dialects.SqlDialects;

public class SmartMybatisInputKeyConfigurationTest extends TestCase {
    private GlobalConfig previousConfig;
    private SqlDialects previousDialect;
    private GenericApplicationContext context;

    @Override
    protected void setUp() {
        previousConfig = SmartConfigHolder.config();
        previousDialect = SmartConfigHolder.getDialect();
        context = new GenericApplicationContext();
    }

    @Override
    protected void tearDown() {
        context.close();
        SmartConfigHolder.init(previousConfig, previousDialect);
    }

    public void testDefaultPreservesInputPrimaryKey() {
        new SmartMybatisInitializer().initialize(context);
        assertTrue(SmartConfigHolder.config().isPreserveInputPrimaryKey());
    }

    public void testSpringConfigurationCanSelectLegacyBehavior() {
        configure(false);
        assertFalse(SmartConfigHolder.config().isPreserveInputPrimaryKey());
    }

    public void testSpringConfigurationCanExplicitlySelectPreservation() {
        configure(true);
        assertTrue(SmartConfigHolder.config().isPreserveInputPrimaryKey());
    }

    private void configure(boolean preserve) {
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                Collections.<String, Object>singletonMap(
                        "spring.mybatis.smart.preserve-input-primary-key", preserve)));
        new SmartMybatisInitializer().initialize(context);
    }
}
