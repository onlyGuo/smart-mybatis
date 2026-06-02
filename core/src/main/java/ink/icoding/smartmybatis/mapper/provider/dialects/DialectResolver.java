package ink.icoding.smartmybatis.mapper.provider.dialects;

import ink.icoding.smartmybatis.mapper.provider.dialects.impl.MysqlDialect;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * SQL 方言解析器
 * <p>
 * 扫描 classpath 上所有带 {@link SmartDialect} 注解的 {@link SqlDialects} 实现类，
 * 根据 JDBC 驱动类名自动匹配对应的方言。未匹配时兜底使用 {@link MysqlDialect}。
 *
 * @author gsk
 */
public class DialectResolver {

    private static final String IMPL_PACKAGE = "ink.icoding.smartmybatis.mapper.provider.dialects.impl";

    /**
     * 根据 JDBC 驱动类名解析对应的 SQL 方言
     *
     * @param driverClassName JDBC 驱动类名，为空时直接返回默认方言
     * @return 匹配的方言实例，未匹配时返回 {@link MysqlDialect}
     */
    public static SqlDialects resolve(String driverClassName) {
        if (driverClassName == null || driverClassName.trim().isEmpty()) {
            return new MysqlDialect();
        }
        String driver = driverClassName.trim();
        try {
            SqlDialects matched = scanAndMatch(driver);
            if (matched != null) {
                return matched;
            }
        } catch (Exception e) {
            // 扫描失败时兜底
        }
        return new MysqlDialect();
    }

    private static SqlDialects scanAndMatch(String driverClassName) throws Exception {
        String packagePath = IMPL_PACKAGE.replace('.', '/');
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = DialectResolver.class.getClassLoader();
        }

        Enumeration<URL> resources = cl.getResources(packagePath);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            String protocol = url.getProtocol();

            if ("file".equals(protocol)) {
                String dirPath = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8.name());
                SqlDialects found = scanDirectory(new File(dirPath), driverClassName);
                if (found != null) return found;
            } else if ("jar".equals(protocol)) {
                SqlDialects found = scanJar(url, packagePath, driverClassName);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static SqlDialects scanDirectory(File dir, String driverClassName) {
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }
        File[] files = dir.listFiles(f -> f.getName().endsWith(".class"));
        if (files == null) return null;

        for (File file : files) {
            String className = IMPL_PACKAGE + "." + file.getName().replace(".class", "");
            SqlDialects matched = tryMatch(className, driverClassName);
            if (matched != null) return matched;
        }
        return null;
    }

    private static SqlDialects scanJar(URL url, String packagePath, String driverClassName) {
        try {
            JarURLConnection conn = (JarURLConnection) url.openConnection();
            try (JarFile jar = conn.getJarFile()) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(packagePath) && name.endsWith(".class") && !entry.isDirectory()) {
                        String className = name.replace('/', '.').replace(".class", "");
                        SqlDialects matched = tryMatch(className, driverClassName);
                        if (matched != null) return matched;
                    }
                }
            }
        } catch (IOException e) {
            // ignore
        }
        return null;
    }

    private static SqlDialects tryMatch(String className, String driverClassName) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!SqlDialects.class.isAssignableFrom(clazz)) {
                return null;
            }
            SmartDialect annotation = clazz.getAnnotation(SmartDialect.class);
            if (annotation == null) {
                return null;
            }
            for (String driver : annotation.value()) {
                if (driver.equals(driverClassName)) {
                    return (SqlDialects) clazz.newInstance();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}
