package com.chengyi.chendb.idx;

import java.util.Arrays;

public class BNode {
    // node所属b树
    BTree tree;
    // 方便裂页溢出时，mid part entry填充到父页
    BNode parent;
    // 存储指向子node的指针
    BNode[] children;
    // 存储node中entry数据
    Entry[] entries;
    // node中最多可以存的entry数量
    int maxEntrySize;
    // 分裂后node总entry的阈值
    int threshold;
    // node中当前entry的数量, max(entrySize) = maxEntrySize，实际数组长度是可以到maxEntrySize + 1.
    int entrySize;
    // 指向子node的指针数量
    int childrenSize;
    // 是否为根节点
    boolean isRoot;

    public BNode(int maxEntrySize, BTree tree) {
        this.tree = tree;
        this.maxEntrySize = maxEntrySize;
        this.threshold = (int) Math.ceil(this.maxEntrySize / 2.0);
        this.entrySize = 0;
        this.childrenSize = 0;
        // m 个 entry 可以拥有 m + 1 个指针
        children = new BNode[this.maxEntrySize + 1];
        // 多出一位是因为裂页时多出的节点也可放入，方便编码
        entries = new Entry[this.maxEntrySize + 1];
        this.isRoot = false;
    }

    /**
     * tips :
     * 1. 若B树不存在这个key,则一定是在叶子结点中进行插入操作。
     * 2. 假设插入的key没有重复
     * @param key
     * @param pointer
     */
    public void insert(Comparable key, Object pointer) {
        Entry entry = new Entry(key, pointer);
        // 如果没有child node 并且 此node未满，则在这个node中插入这个entry
        if (childrenSize == 0) {
            if (entrySize < maxEntrySize) {
                int pos = find(key);
                arraycopy(pos, pos + 1, entrySize - pos);
                entries[pos] = entry;
                entrySize++;
                System.out.println("成功插入该页");
            } else {
                // TODO 位置不够，开始裂页；
                // 首先先将新节点放入即将满的entries数组， 然后进行一组find排序。
                // 此时entries中有 m + 1 个元素(m = 4，即最终逻辑上只能容纳四个元素)，取 2 1 2 作为新的裂页方案。
                int pos = find(key);
                System.out.println("pos : " + pos);
                arraycopy(pos, pos + 1, entrySize - pos);
                // 此时entries满
                entries[pos] = entry;
                // 作为左半部分长度
                int leftPartLength = threshold;
                // 作为中间entry的下标
                int midPartUnderCase = leftPartLength;
                // 作为右半部分开始下标
                int rightPartStart = midPartUnderCase + 1;

                Entry[] leftPart = new Entry[this.maxEntrySize + 1];
                Entry midPart = entries[midPartUnderCase];
                Entry[] rightPart = new Entry[this.maxEntrySize + 1];


                System.arraycopy(entries, 0, leftPart, 0, leftPartLength);
                setNulls(entries, 0, leftPartLength);
                System.arraycopy(entries, rightPartStart, rightPart, 0, entries.length - rightPartStart);
                setNulls(entries, rightPartStart, entries.length - 1);

                System.out.println(Arrays.toString(leftPart));
                System.out.println(midPart);
                System.out.println(Arrays.toString(rightPart));

                // 5 节点 6 实际容量 1 2 3 4 5 6 -> 1 2 3 | 4 | 5 6
                BNode leftNode = new BNode(maxEntrySize, tree);
                leftNode.setEntries(leftPart);
                BNode rightNode = new BNode(maxEntrySize, tree);
                rightNode.setEntries(rightPart);


                // 若父节点不为空，就把 midpart 插入父节点， midpart 左右两侧索引指向 leftpart 和 rightpart 。 leftpart 和 rightpart parent 指向 parent
                if (this.parent != null) {
                    leftNode.setParent(this.parent);
                    rightNode.setParent(this.parent);

                    this.parent.insert(midPart.getKey(), midPart.getPointer());
                    // TODO update parent index


                } else {
                    // 若父节点为空，那么说明此时节点就是根节点，并且此时根节点只有一个元素。 只需要为此节点插入根索引节点即可.
                    leftNode.setParent(this);
                    rightNode.setParent(this);

                    setLeftNode(this, 0);
                    setRightNode(this, 0);
                    // 重新计算entry数量
                    this.entrySize = 1;
                    recountEntrySize(leftNode);
                    recountEntrySize(rightNode);
                }
            }
        }
    }

    private void recountEntrySize(BNode bNode) {
        Entry[] entries = bNode.entries;
        int cnt = 0;
        for (Entry entry : entries) {
            if (entry == null) break;
            cnt ++;
        }
        bNode.entrySize = cnt;
    }

    public Object get(Object key) {

        return null;
    }

    public void remove(Object key) {

    }

    /**
     * 返回某个key需要插入的下标
     *
     * @param key
     * @return
     */
    @SuppressWarnings("unchecked")
    private int find(Comparable key) {
        // 省略最后一位，最后一位留给分裂页时使用
        for (int i = 0; i <= maxEntrySize; i++) {
            if (entries[i] == null) return i;
            // 如果大于等于就继续往后找位置
            if (key.compareTo(entries[i].key) < 0) {
                return i;
            }
        }
        return -1;
    }

    private BNode getLeftNode(BNode node, int pos) {
        return node.getChildren()[pos];
    }

    private BNode getRightNode(BNode node, int pos) {
        return node.getChildren()[pos + 1];
    }

    private void setLeftNode(BNode node, int pos) {
        node.getChildren()[pos] = node;
    }

    private void setRightNode(BNode node, int pos) {
        node.getChildren()[pos + 1] = node;
    }

    private void arraycopy(int start, int newStart, int len) {
        System.arraycopy(this.entries, start, this.entries, newStart, len);
    }

    private void setNulls(Entry[] src, int start, int end) {
        for (int i = start; i <= end; i++) {
            src[i] = null;
        }
    }

    public BNode[] getChildren() {
        return children;
    }

    public void setChildren(BNode[] children) {
        this.children = children;
    }

    public int getEntrySize() {
        return entrySize;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setIsRoot(boolean isRoot) {
        this.isRoot = isRoot;
    }

    public BNode getParent() {
        return parent;
    }

    public void setParent(BNode parent) {
        this.parent = parent;
    }

    public Entry[] getEntries() {
        return entries;
    }

    public void setEntries(Entry[] entries) {
        this.entries = entries;
    }

    static class Entry {
        // 索引值
        Comparable key;
        // 指向数据的指针，这里直接记为offset
        Object pointer;

        public Entry(Comparable key, Object pointer) {
            this.key = key;
            this.pointer = pointer;
        }

        public Comparable getKey() {
            return key;
        }

        public void setKey(Comparable key) {
            this.key = key;
        }

        public Object getPointer() {
            return pointer;
        }

        public void setPointer(Object pointer) {
            this.pointer = pointer;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "key=" + key +
                    ", pointer=" + pointer +
                    '}';
        }
    }
}
