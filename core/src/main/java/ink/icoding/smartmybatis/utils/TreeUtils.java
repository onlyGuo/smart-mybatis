package ink.icoding.smartmybatis.utils;

import ink.icoding.smartmybatis.entity.SmartTreeNode;
import ink.icoding.smartmybatis.entity.po.PO;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

public class TreeUtils {
    private TreeUtils() {}

    /**
     * 构建树形结构
     * @param items         列表
     * @param idGetter      主键
     * @param parentGetter  内键
     * @param sorter        排序
     */
    public static <T extends PO, K extends Serializable>
    List<SmartTreeNode<T, K>> buildTree(List<T> items,
                                        Function<T, K> idGetter,
                                        Function<T, K> parentGetter,
                                        Comparator<SmartTreeNode<T, K>> sorter) {

        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        Map<K, SmartTreeNode<T, K>> nodes = new LinkedHashMap<>();
        List<SmartTreeNode<T, K>> roots = new ArrayList<>();

        // 1) 创建节点
        for (T item : items) {
            K id = idGetter.apply(item);
            K pid = parentGetter.apply(item);
            SmartTreeNode<T, K> node = new SmartTreeNode<>(id, pid, item);
            node.setLeaf(true);
            node.setDepth(0);
            node.setPath(id == null ? "" : String.valueOf(id));
            nodes.put(id, node);
        }

        // 2) 挂接父子关系
        for (SmartTreeNode<T, K> node : nodes.values()) {
            K pid = node.getParentId();
            if (pid == null || !nodes.containsKey(pid)) {
                // 作为根
                roots.add(node);
            } else {
                SmartTreeNode<T, K> parent = nodes.get(pid);
                parent.addChild(node);
            }
        }

        // 3) 排序（可选）
        if (sorter != null) {
            for (SmartTreeNode<T, K> r : roots) {
                r.sortRecursively(sorter);
            }
        }

        return roots;
    }
}
