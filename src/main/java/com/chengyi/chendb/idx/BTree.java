package com.chengyi.chendb.idx;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 1. 每个节点至少需要容纳一半大小的entry，例如总容量为11.那么至少需要容纳 ceil(11 / 2) = 6
 * 2. Root 最少可容纳 1 个entry
 * 3. 自下而上构建
 * 4. 所有叶子节点都在同一层
 */
public class BTree implements Index {
    int level;
    public static final int defaultLevel = 6;
    BNode root;
    String keyName;

    public BTree(String keyName) {
        this.keyName = keyName;
        this.level = defaultLevel;
        root = new BNode(this.level, this);
    }

    public BTree(String keyName, int level) {
        this.keyName = keyName;
        this.level = level;
        root = new BNode(this.level, this);
        root.setIsRoot(true);
    }

    static int cnt = 1;
    public void insertOrUpdate(Comparable key, Object pointer) {
        System.out.println("============= - insert start");
        System.out.println(cnt + "插入数据索引值：" + key);
        cnt++;
        root.insertOrUpdate(key, pointer);
        // bfsPrintf(root);
        System.out.println("============= - insert end");
    }

    @Override
    public Object get(Object key) {
        return root.get(key);
    }

    @Override
    public void remove(Object key) {
        root.remove(key);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public BNode getRoot() {
        return root;
    }

    public void setRoot(BNode root) {
        this.root = root;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    static Queue<BNode> queue = new LinkedList<>();
    static Queue<Integer> levelQueue = new LinkedList<>();
    public void bfsPrintf(BNode root) {
        queue.offer(root);
        levelQueue.offer(1);
        while (!queue.isEmpty()) {
            BNode curNode = queue.poll();
            Integer level = levelQueue.poll();
            System.out.printf("level %d : %s%n", level, Arrays.toString(curNode.getEntries()));
            BNode[] children = curNode.getChildren();
            if (children != null) {
                for (BNode child : children) {
                    if (child != null) {
                        queue.offer(child);
                        levelQueue.offer(level + 1);
                    }
                }
            }
        }
        queue.clear();
        levelQueue.clear();
    }
}
