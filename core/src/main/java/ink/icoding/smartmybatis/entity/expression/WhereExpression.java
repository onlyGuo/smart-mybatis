package ink.icoding.smartmybatis.entity.expression;

import ink.icoding.smartmybatis.entity.po.PO;

/**
 * Where 条件表达式
 * @author gsk
 */
public class WhereExpression implements Expression<PO>{
    private Where where;
    private Link link;

    public WhereExpression(Where where, Link link) {
        this.where = where;
        this.link = link;
    }

    public Where getWhere() {
        return where;
    }

    public void setWhere(Where where) {
        this.where = where;
    }

    public Link getLink() {
        return link;
    }

    public void setLink(Link link) {
        this.link = link;
    }
}
