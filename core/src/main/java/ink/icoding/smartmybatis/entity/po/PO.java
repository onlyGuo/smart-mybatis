package ink.icoding.smartmybatis.entity.po;

import java.lang.reflect.Field;

public class PO {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append(" {");
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                sb.append("\n  ").append(field.getName()).append(": ").append(field.get(this));
            } catch (IllegalAccessException e) {
                sb.append("\n  ").append(field.getName()).append(": ").append("ACCESS ERROR");
            }
        }
        sb.append("\n}");
        return sb.toString();
    }
}
