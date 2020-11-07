package com.chengyi.chendb.idx;

public class BNode {
    BTree tree;
    // 方便裂页溢出时，mid part entry填充到父页
    BNode parent;
    BNode[] children;
    Entry[] entries;
    int maxEntrySize;
    int threshold;
    int size;
    boolean isRoot;

    public BNode(int maxEntrySize, BTree tree) {
        this.tree = tree;
        this.maxEntrySize = maxEntrySize;
        this.threshold = (int) Math.ceil(this.maxEntrySize / 2.0);
        this.size = 0;
        children = new BNode[this.maxEntrySize + 1];
        entries = new Entry[this.maxEntrySize + 1];
    }

    public void insert(Comparable key, Object pointer) {
        Entry entry = new Entry(key, pointer);
        // 此node未满一半，那必须插入这个node
        if (this.size < this.threshold) {
            int pos = find(key);
            arraycopy(pos, pos + 1, this.size - pos);
            entries[pos] = entry;
            this.size++;
        }
    }

    public Object get(Object key) {

        return null;
    }

    public void remove(Object key) {

    }

    @SuppressWarnings("unchecked")
    private int find(Comparable key) {
        // 省略最后一位，最后一位留给分裂页时使用
        int len = entries.length - 1;
        for (int i = 0; i < len; i++) {
            // 如果大于等于就继续往后找位置
            if (key.compareTo(entries[i]) < 0) {
                return i;
            }
        }
        return -1;
    }

    private BNode left(int pos) {
        return children[pos];
    }

    private BNode right(int pos) {
        return children[pos + 1];
    }

    private void arraycopy(int start, int newStart, int len) {
        System.arraycopy(this.entries, start, this.entries, newStart, len);
    }

    public int getSize() {
        return size;
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

    static class Entry {
        Comparable key;
        Object pointer;

        public Entry(Comparable key, Object pointer) {
            this.key = key;
            this.pointer = pointer;
        }

        public Object getKey() {
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
    }
}
