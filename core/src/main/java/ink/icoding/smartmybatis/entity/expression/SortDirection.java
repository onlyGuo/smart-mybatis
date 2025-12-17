package ink.icoding.smartmybatis.entity.expression;

/**
 * 排序方向枚举
 * @author gsk
 */
public enum SortDirection {

    ASC("ASC", "正序"),
    DESC("DESC", "倒序");

    private final String code;
    private final String description;
    SortDirection(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
