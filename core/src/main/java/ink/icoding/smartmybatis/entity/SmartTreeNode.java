package ink.icoding.smartmybatis.entity;

import ink.icoding.smartmybatis.entity.po.PO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class SmartTreeNode<T extends PO, K extends Serializable> implements Serializable {

    private K id;
    private K parentId;

    private T data;

    private List<SmartTreeNode<T, K>> children = new ArrayList<>();

    private SmartTreeNode<T, K> parent;

    // 根为 0
    private int depth;
    // 例如 "root/child/grandchild"
    private String path;
    // children 为空时为 true
    private boolean leaf;

    public SmartTreeNode() {}

    public SmartTreeNode(K id, K parentId, T data) {
        this.id = id;
        this.parentId = parentId;
        this.data = data;
    }

    // ———— 基本访问器 ————
    public K getId() { return id; }
    public void setId(K id) { this.id = id; }

    public K getParentId() { return parentId; }
    public void setParentId(K parentId) { this.parentId = parentId; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public List<SmartTreeNode<T, K>> getChildren() { return children; }
    public void setChildren(List<SmartTreeNode<T, K>> children) {
        this.children = children != null ? children : new ArrayList<>();
        this.leaf = this.children.isEmpty();
    }

    public SmartTreeNode<T, K> getParent() { return parent; }
    public void setParent(SmartTreeNode<T, K> parent) { this.parent = parent; }

    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public boolean isLeaf() { return leaf; }
    public void setLeaf(boolean leaf) { this.leaf = leaf; }

    // ———— 常用辅助方法 ————
    public boolean isRoot() {
        return parent == null || parentId == null;
    }

    public void addChild(SmartTreeNode<T, K> child) {
        if (child == null) {
            return;
        }
        children.add(child);
        child.setParent(this);
        child.setDepth(this.depth + 1);
        child.setPath(this.path == null ?
                String.valueOf(child.id) : this.path + "/" + child.id);
        this.leaf = false;
    }

    public void sortRecursively(Comparator<SmartTreeNode<T, K>> comparator) {
        if (children == null || children.isEmpty()){
            return;
        }
        Collections.sort(children, comparator);
        for (SmartTreeNode<T, K> c : children) {
            c.sortRecursively(comparator);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SmartTreeNode)) {
            return false;
        }
        SmartTreeNode<?, ?> that = (SmartTreeNode<?, ?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        // 利用 PO 的 toString 展示 data，避免递归打印 children
        return "SmartTreeNode{" +
                "id=" + id +
                ", parentId=" + parentId +
                ", depth=" + depth +
                ", leaf=" + leaf +
                ", data=" + (data == null ? "null" : data.toString()) +
                '}';
    }
}
