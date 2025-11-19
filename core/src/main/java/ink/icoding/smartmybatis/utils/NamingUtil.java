package ink.icoding.smartmybatis.utils;

import ink.icoding.smartmybatis.entity.po.enums.TableField;

/**
 * 命名工具类
 * @author gsk
 */
public class NamingUtil {

    /**
     * 驼峰命名法转下划线小写
     * @param text 驼峰命名法字符串
     * @return 下划线小写字符串
     */
    public static String camelToUnderlineLower(String text){
        if(text == null|| text.trim().isEmpty()){
            return "";
        }
        int len=text.length();
        StringBuilder sb = new StringBuilder(len);
        for(int i = 0; i < len; i++){
            char c = text.charAt(i);
            if(Character.isUpperCase(c)){
                if(i != 0){
                    sb.append("_");
                }
                sb.append(Character.toLowerCase(c));
            }else{
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 驼峰命名法转下划线大写
     * @param simpleName 驼峰命名法字符串
     * @return 下划线大写字符串
     */
    public static String camelToUnderlineUpper(String simpleName) {
        if(simpleName == null|| simpleName.trim().isEmpty()){
            return "";
        }
        int len = simpleName.length();
        StringBuilder sb = new StringBuilder(len);
        for(int i = 0; i < len; i++){
            char c = simpleName.charAt(i);
            if(Character.isUpperCase(c)){
                if(i != 0){
                    sb.append("_");
                }
                sb.append(c);
            }else{
                sb.append(Character.toUpperCase(c));
            }
        }
        return sb.toString();
    }

    public static String javaTypeToSqlType(Class<?> type, TableField tableField) {
        int length = 255;
        if (null != tableField) {
            if (tableField.columnType() != null && !tableField.columnType().isEmpty()) {
                return tableField.columnType();
            }
            length = tableField.length();
        }
        if (0 == length){
            length = 255;
        }
        if (type == Integer.class || type == int.class) {
            return "INT(11)";
        } else if (type == Long.class || type == long.class) {
            return "BIGINT";
        } else if (type == Boolean.class || type == boolean.class) {
            return "TINYINT(1)";
        } else if (type == java.util.Date.class || type == java.sql.Date.class) {
            return "DATETIME";
        } else if (type == Double.class || type == double.class) {
            return "DECIMAL(17, 6)";
        } else if (type == Float.class || type == float.class) {
            return "DECIMAL(17, 1)";
        } else if (type == java.math.BigDecimal.class) {
            return "DECIMAL(17, 6)";
            // 是否是枚举类型
        } else if (type.isEnum()) {
            return "VARCHAR(255)";
        }else {
            if (null != tableField && tableField.json()){
                return "LONGTEXT";
            }
        }

        // 默认按字符串处理
        if (length <= 16383){
            return "VARCHAR(" + length + ")";
        }else if (length <= 65535){
            return "TEXT";
        }else{
            return "LONGTEXT";
        }
    }
}
