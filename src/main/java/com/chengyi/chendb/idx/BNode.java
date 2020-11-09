package com.chengyi.chendb.idx;

import java.util.Arrays;

public class BNode {
    // (immutable) node所属b树
    BTree tree;
    // (mutable) 方便裂页溢出时，mid part entry填充到父页
    BNode parent;
    // (mutable) 存储指向子node的指针
    BNode[] children;
    // (mutable) 存储node中entry数据
    Entry[] entries;
    // (immutable) node中最多可以存的entry数量
    int maxEntrySize;
    // (mutable) node中当前entry的数量, max(entrySize) = maxEntrySize，实际数组长度是可以到maxEntrySize + 1.
    int entrySize;
    // (mutable) 指向子node的指针数量
    int childrenSize;
    // (mutable) 是否为根节点
    boolean isRoot;
    // (mutable) 分裂后的页
    BNode[] splittedPart;

    public BNode(int maxEntrySize, BTree tree) {
        this.tree = tree;
        this.maxEntrySize = maxEntrySize;
        this.entrySize = 0;
        this.childrenSize = 0;
        // m 个 entry 可以拥有 m + 1 个指针
        children = new BNode[this.maxEntrySize + 2];
        // 多出一位是因为裂页时多出的节点也可放入，方便编码
        entries = new Entry[this.maxEntrySize + 1];
        this.isRoot = false;
    }

    /**
     * tips :
     * 1. 若B树不存在这个key,则一定是在叶子结点中进行插入操作。
     * 2. 假设插入的key没有重复
     *
     * @param key
     * @param pointer
     */
    public void insertOrUpdate(Comparable key, Object pointer) {
        Entry entry = new Entry(key, pointer);
        // 不是叶子节点
        if (childrenSize != 0) {
            int pos = findInsertIndex(key);
            BNode nextNode;
            if (pos == maxEntrySize) {
                nextNode = getRightNode(this, pos);
            } else {
                // 只要找到key相同，就原地更新
                if (entries[pos].key.compareTo(key) == 0) {
                    entries[pos].pointer = pointer;
                    return ;
                }
                nextNode = getLeftNode(this, pos);
            }
            nextNode.insertOrUpdate(key, pointer);
        } else {
           directInsert2BNode(this, entry);
        }
    }

    private void directInsert2BNode(BNode targetNode, Entry entry) {
        // 是叶子节点，若已经满了，则需要裂页
        if (entrySize == maxEntrySize) {
            int pos = findInsertIndex(entry.getKey());
            // exp1 : [1,2,4,5,|]  插入 3, pos = 2, so len = 4 - 2 = 2 : true
            arraycopy(pos, pos + 1, entrySize - pos);
            // 先置入对应entries
            entries[pos] = entry;
            // 此时entries为【1,2,3,4,5】
            // 建立新页
            Entry[] leftNodeData = new Entry[this.entries.length];
            Entry[] rightNodeData = new Entry[this.entries.length];
            // 拷贝新页
            BNode leftNode = new BNode(maxEntrySize, tree);
            BNode rightNode = new BNode(maxEntrySize, tree);
            int leftLen = entries.length / 2;
            Entry midEntry = entries[leftLen];
            System.arraycopy(entries, 0, leftNodeData, 0, leftLen);
            System.arraycopy(entries, leftLen + 1, rightNodeData, 0, entries.length - leftLen - 1);
            leftNode.setEntries(leftNodeData);
            rightNode.setEntries(rightNodeData);
            // 重新计算裂页后新的entrySize
            recountEntrySize(leftNode);
            recountEntrySize(rightNode);

            splittedPart = new BNode[2];
            splittedPart[0] = leftNode;
            splittedPart[1] = rightNode;

            // 如果需要重新构造根节点，因为同时又是叶子节点，所以可以肯定的是此时newRoot只有两个子节点
            if (isRoot) {
                BNode newRoot = new BNode(maxEntrySize, tree);
                tree.setRoot(newRoot);
                newRoot.getEntries()[0] = midEntry;
                setLeftNode(newRoot, leftNode, 0);
                setRightNode(newRoot, rightNode, 0);
            } else {
                // 是正常的叶节点，那么必定有parent节点，不需要判空
                BNode[] children = this.parent.getChildren();
                int idx = 0;
                for (; idx < children.length; idx++) {
                    if (children[idx].equals(this)) {
                        break;
                    }
                }
                System.arraycopy(children, idx, children, idx + 1, childrenSize - idx);
                children[idx] = leftNode;
                children[idx+1] = rightNode;
                childrenSize++;

                // 至此，即使是进行parent节点插入midEntry前，parent的children已经安排好了
                directInsert2BNode(this.parent, midEntry);
                // 此时我具有的完备数据
                // 1. parent层的完整children指针
                // 2. 可能获得的是 加入parent分裂过后的两个part
                // 我要做的事重新设置可能的两个part的children　array以及重新计算他们的childrenSize
                if (this.parent.splittedPart != null) {
                    int childIdx = 0;
                    for (int i = 0; i < 2; i++) {
                        BNode curNode = splittedPart[i];
                        BNode[] curNodeChildren = curNode.getChildren();
                        for (int j = 0; j <= entrySize; j++) {
                            curNodeChildren[j] = this.parent.children[childIdx];
                            childIdx++;
                            if (this.parent.children[childIdx].equals(leftNode)) {
                                leftNode.setParent(curNode);
                            } else if (this.parent.children[childIdx].equals(rightNode)) {
                                rightNode.setParent(curNode);
                            }
                        }
                        //　重新对分裂后得到的node计算childrenSize
                        recountChildrenSize(curNode);
                    }
                    this.parent.splittedPart = null;
                } else {
                    leftNode.setParent(this.parent);
                    rightNode.setParent(this.parent);
                }
            }
        } else {
            // 叶子节点未满，直接寻找位置插入
            int pos = findInsertIndex(entry.getKey());
            arraycopy(pos, pos + 1, entrySize - pos);
            entries[pos] = entry;
            entrySize += 1;
        }
    }

    private void recountEntrySize(BNode bNode) {
        Entry[] entries = bNode.entries;
        int cnt = 0;
        for (Entry entry : entries) {
            if (entry == null) break;
            cnt++;
        }
        bNode.entrySize = cnt;
    }

    private void recountChildrenSize(BNode bNode) {
        int cnt = 0;
        BNode[] children = bNode.getChildren();
        for (BNode child : children) {
            if (child == null) break;
            cnt++;
        }
        bNode.childrenSize = cnt;
    }

    public Object get(Object key) {

        return null;
    }

    public void remove(Object key) {

    }

    /**
     * 返回第一个比 key 大的值的下标； 若所有元素都比 key 小则返回当前元素数量（最大非空元素下标 +　１）；
     *
     * @param key
     * @return
     */
    @SuppressWarnings("unchecked")
    private int findInsertIndex(Comparable key) {
        for (int i = 0; i <= maxEntrySize; i++) {
            if (entries[i] == null) return i;
            // 如果大于等于就继续往后找位置
            if (key.compareTo(entries[i].key) < 0) {
                return i;
            }
        }
        return -1;
    }

    private int findEqualPos(Comparable key) {
        // 包括多出来的裂页临时值
        for (int i = 0; i <= maxEntrySize; i++) {
            if (key.compareTo(entries[i].key) == 0) {
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

    private void setLeftNode(BNode curNode, BNode childNode, int pos) {
        curNode.getChildren()[pos] = childNode;
    }

    private void setRightNode(BNode curNode, BNode childNode, int pos) {
        curNode.getChildren()[pos + 1] = childNode;
    }

    private void arraycopy(int start, int newStart, int len) {
        System.arraycopy(this.entries, start, this.entries, newStart, len);
    }

    private void setNulls(Entry[] src, int start, int end) {
        for (int i = start; i <= end; i++) {
            src[i] = null;
        }
    }

    private String toString(Entry[] arr) {
        return Arrays.toString(arr);
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
