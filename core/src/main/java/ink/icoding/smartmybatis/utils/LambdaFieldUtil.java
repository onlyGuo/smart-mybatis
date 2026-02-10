package ink.icoding.smartmybatis.utils;

import ink.icoding.smartmybatis.entity.expression.SFunction;
import ink.icoding.smartmybatis.entity.po.PO;

import java.lang.annotation.Annotation;
import java.lang.invoke.*;
import java.lang.reflect.*;

/**
 * Lambda字段工具类
 * @author gsk
 */
public class LambdaFieldUtil {

    /**
     * 获取Lambda表达式对应的字段名
     */
    public static String getFieldName(SFunction<? extends PO, ?> getter) {
        try {
            // 1. 获取SerializedLambda
            Method writeReplace = getter.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            Object serializedForm = writeReplace.invoke(getter);

            if (!(serializedForm instanceof SerializedLambda)) {
                throw new RuntimeException("Not a lambda");
            }
            SerializedLambda lambda = (SerializedLambda) serializedForm;

            // 2. 获取方法名
            String methodName = lambda.getImplMethodName();

            // 3. 推断字段名
            String fieldName = null;
            if (methodName.startsWith("get")) {
                fieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
            } else if (methodName.startsWith("is")) {
                fieldName = methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
            } else {
                fieldName = methodName;
            }
            return fieldName;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Field getField(SFunction<? extends PO, ?> getter, Class<? extends PO> clazz) {
        String fieldName = getFieldName(getter);
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <A extends Annotation> A getFieldAnnotation(SFunction<? extends PO, ?> getter, Class<? extends PO> clazz, Class<A> annotationClass) {
        Field field = getField(getter, clazz);
        return field.getAnnotation(annotationClass);
    }

    public static Field getField(SFunction<? extends PO, ?> func) {
        try {
            Method writeReplace = func.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            Object serializedForm = writeReplace.invoke(func);

            if (!(serializedForm instanceof SerializedLambda)) {
                throw new RuntimeException("Not a lambda");
            }
            SerializedLambda lambda = (SerializedLambda) serializedForm;

            // 2. 获取方法名
            String methodName = lambda.getImplMethodName();
            String implClass = lambda.getImplClass();
            Class<?> clazz = Class.forName(implClass.replace('/', '.'));
            // 3. 推断字段名
            String fieldName = null;
            if (methodName.startsWith("get")) {
                fieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
            } else if (methodName.startsWith("is")) {
                fieldName = methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
            } else {
                fieldName = methodName;
            }
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassNotFoundException |
                 NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

    }

    public static Class<? extends PO> getPoClass(SFunction<? extends PO, ?> func) {
        try {
            Method writeReplace = func.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            Object serializedForm = writeReplace.invoke(func);

            if (!(serializedForm instanceof SerializedLambda)) {
                throw new RuntimeException("Not a lambda");
            }
            SerializedLambda lambda = (SerializedLambda) serializedForm;

            String implClass = lambda.getImplClass();
            return (Class<? extends PO>) Class.forName(implClass.replace('/', '.'));
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
