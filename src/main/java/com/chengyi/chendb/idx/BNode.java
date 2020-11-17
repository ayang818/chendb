package com.chengyi.chendb.idx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class BNode {
    // (immutable) node所属b树
    private BTree tree;
    // (mutable) 方便裂页溢出时，mid part entry填充到父页
    private BNode parent;
    // (mutable) 存储指向子node的指针
    private BNode[] children;
    // (mutable) 存储node中entry数据
    private Entry[] entries;
    // (immutable) node中最多可以存的entry数量
    private int maxEntrySize;
    // (mutable) node中当前entry的数量, max(entrySize) = maxEntrySize，实际数组长度是可以到maxEntrySize + 1.
    private int entrySize;
    // (mutable) 指向子node的指针数量
    private int childrenSize;
    // (mutable) 是否为根节点
    private boolean isRoot;
    // (mutable) 分裂后的页
    private BNode[] splittedPart;

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
        // TODO #bug 寻找错误# 不是叶子节点
        if (childrenSize != 0) {
            int[] posRes = findInsertIndex(this, key);
            // 只要找到key相同，就原地更新并退出
            if (posRes[1] == 0) {
                entries[posRes[0]].pointer = pointer;
                return;
            }
            BNode nextNode;
            // if (posRes[0] == maxEntrySize) {
            //     nextNode = getRightNode(this, posRes[0]);
            // } else {
            //     nextNode = getLeftNode(this, posRes[0]);
            // }
            nextNode = getLeftNode(this, posRes[0]);
            nextNode.insertOrUpdate(key, pointer);
        } else {
            // 找到了目标大小叶子节点，直接将entry插入此叶子节点
            this.directInsert2BNode(entry);
        }
    }

    /**
     * 直接插入目标BNode，不再进行向下寻找；若目标BNode未满，则直接插入。若目标BNode1已满，则插入后裂页。
     * WARNING: 此处函数主语为targetNode，而targetNode和this有些时候是不同的，此方法中的this指的都是第一次找到时候的叶节点
     *
     * @param entry
     */
    private void directInsert2BNode(Entry entry) {
        int[] posRes = findInsertIndex(this, entry.getKey());
        // System.out.println(toString(targetNode.entries));
        // System.out.println("entrySize" + targetNode.entrySize);
        if (entrySize == maxEntrySize) {
            if (posRes[1] == 0) {
                entries[posRes[0]] = entry;
            }
            // exp1 : [1,2,4,5,|]  插入 3, pos = 2, so len = 4 - 2 = 2 : true
            this.arraycopy(posRes[0], posRes[0] + 1, entrySize - posRes[0]);
            // 先置入对应entries
            entries[posRes[0]] = entry;
            // 此时entries为【1,2,3,4,5】
            // 建立新页
            Entry[] leftNodeData = new Entry[this.entries.length];
            Entry[] rightNodeData = new Entry[this.entries.length];
            // 拷贝新页
            BNode leftNode = new BNode(maxEntrySize, tree);
            BNode rightNode = new BNode(maxEntrySize, tree);
            int leftLen = this.entries.length / 2;
            Entry midEntry = entries[leftLen];
            System.arraycopy(entries, 0, leftNodeData, 0, leftLen);
            System.arraycopy(entries, leftLen + 1, rightNodeData, 0, entries.length - leftLen - 1);

            // System.out.println("裂页左部分　：" + toString(leftNodeData));
            // System.out.println("裂页中间部分　：" + midEntry);
            // System.out.println("裂页右部分　：" + toString(rightNodeData));

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
                newRoot.setIsRoot(true);
                newRoot.getEntries()[0] = midEntry;
                setLeftNode(newRoot, leftNode, 0);
                setRightNode(newRoot, rightNode, 0);
                leftNode.setParent(newRoot);
                rightNode.setParent(newRoot);
                newRoot.setParent(null);
                recountEntrySize(newRoot);
                recountChildrenSize(newRoot);
            } else {
                // 是正常的叶节点，那么必定有parent节点，不需要判空
                // TODO wait to fix multi split error
                BNode[] brothers = this.parent.getChildren();
                for (BNode bro : brothers) {
                    if (bro != null)
                        System.out.println(toString(bro.entries));
                }
                int idx = 0;
                for (; idx < brothers.length; idx++) {
                    // 找出targetNode在兄弟节点中的排位
                    if (brothers[idx].equals(this)) {
                        break;
                    }
                }
                System.out.println("idx" + idx);
                System.out.println(this.parent.childrenSize);

                System.out.println(brothers.length);
                System.arraycopy(brothers, idx, brothers, idx + 1, this.parent.childrenSize - idx);
                // 由于targetNode已经裂页为leftNode和rightNode，所以需要先将leftNode和rightNode放到children数组中的合适位置
                brothers[idx] = leftNode;
                brothers[idx + 1] = rightNode;
                this.parent.childrenSize++;

                // 至此，即使是进行parent节点插入midEntry前，parent的children已经安排好了
                this.parent.directInsert2BNode(midEntry);
                // 此时具有的完备数据
                // 1. parent层的完整children指针
                // 2. 可能获得的是 加入parent分裂过后的两个part
                // 我要做的事重新设置可能的两个part的children　array以及重新计算他们的childrenSize
                if (this.parent.splittedPart != null) {
                    int childIdx = 0;
                    System.out.println("here use brother");
                    // 将parent分裂后的两个part重新构造children数组；设置parent指针
                    for (int i = 0; i < 2; i++) {
                        BNode curNode = this.parent.splittedPart[i];
                        recountEntrySize(curNode);
                        BNode[] curNodeChildren = curNode.getChildren();
                        for (int j = 0; j <= curNode.entrySize; j++) {
                            curNodeChildren[j] = brothers[childIdx];
                            brothers[childIdx].parent = curNode;
                            childIdx++;
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
            System.out.println("direct insert");
            // 叶子节点未满，直接寻找位置插入
            if (posRes[1] == 0) {
                this.entries[posRes[0]] = entry;
                return ;
            }
            // System.out.println(toString(targetNode.entries));
            // System.out.println(posRes[0]);
            this.arraycopy(posRes[0], posRes[0] + 1, this.entrySize - posRes[0]);
            // TODO 类方法的调用必须要注意的调用方法的主语，以免出现不同对象调用目标方法导致结果不符合预期
            // System.out.println(toString(targetNode.entries));
            this.entries[posRes[0]] = entry;
            this.entrySize += 1;
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

    public Object get(Comparable key) {
        int[] posRes = findInsertIndex(this, key);
        // 只要找到key相同，返回对应offset
        if (posRes[1] == 0) {
            return entries[posRes[0]].pointer;
        }
        if (this.childrenSize == 0) {
            return null;
        }
        BNode nextNode;
        // if (posRes[0] == maxEntrySize) {
        //     nextNode = getRightNode(this, posRes[0]);
        // } else {
        //     nextNode = getLeftNode(this, posRes[0]);
        // }
        nextNode = getLeftNode(this, posRes[0]);
        return nextNode.get(key);
    }

    public void remove(Object key) {
        
    }

    /**
     * 返回第一个比 key 大的值的下标； 若所有元素都比 key 小则返回当前元素数量（最大非空元素下标 +　１）；
     *
     * TODO 切换成使用二分查找
     *
     * @param key
     * @return
     */
    @SuppressWarnings("unchecked")
    private int[] findInsertIndex(BNode targetNode, Comparable key) {
        int left = 0;
        int right = targetNode.entrySize;
        int mid;
        while (left < right) {
            mid = (left + right) / 2;
            Entry entry = targetNode.entries[mid];
            if (entry == null) return new int[]{mid, 1};
            int cmp = entry.key.compareTo(key);
            if (cmp > 0) {
                right = mid - 1;
            } else if (cmp < 0) {
                left = mid + 1;
            } else {
                return new int[]{mid, 0};
            }
        }
        return new int[]{0, 1};
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

    /**
     * @param start
     * @param newStart
     * @param len
     */
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

    @Override
    public String toString() {
        return "BNode{" +
                " children=" + Arrays.toString(children) +
                ", entries=" + Arrays.toString(entries) +
                ", entrySize=" + entrySize +
                ", childrenSize=" + childrenSize +
                ", isRoot=" + isRoot +
                "}\n";
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
