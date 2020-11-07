package com.chengyi.chendb.data;

import com.chengyi.chendb.idx.BTree;
import com.chengyi.chendb.idx.Index;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 作为数据表模拟类，可以拥有多条索引
 */
public class Table<T> {
    List<Index> indexes;
    // 模拟磁盘的行为，主要操作有insert，get；remove只作标记
    List<T> disk;
    // 表名
    String tableName;

    public Table(String tableName, int basicSize) {
        this.tableName = tableName;
        disk = new ArrayList<>(basicSize);
        indexes = new ArrayList<>();
    }

    /**
     * 线程不安全，并发插入会导致错误
     * @param record
     */
    public void insertRecord(T record) {
        // 先插入磁盘
        disk.add(record);
        System.out.println("插入数据到磁盘成功");
        
        // 记录在数组中的偏移量，实际上应该是(track, sector, offset)组成的三元组，放在文件系统中就是数据在对应文件的偏移量
        int offset = disk.size() - 1;
        // 维护索引
        String keyName;
        Comparable keyValue = null;
        for (Index index : indexes) {
            keyName = index.getKeyName();
            // 通过反射获取数据索引值
            try {
                Field indexField = record.getClass().getDeclaredField(keyName);
                indexField.setAccessible(true);
                keyValue = (Comparable) indexField.get(record);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }
            // 插入keyValue和在磁盘中的offset
            // 反射获取到的value值
            System.out.println("插入数据索引值：" + keyValue);
            index.insertOrUpdate(keyValue, offset);
        }
    }


    /**
     * @param fieldName 索引字段名
     * @return
     */
    public void createIndexOnField(String fieldName) {
        createIndexOnFiledWithLevel(fieldName, -1);
    }

    public void createIndexOnFiledWithLevel(String fieldName, int level) {
        // TODO 读出已有的数据，然后建立B树
        // 默认为数据为空，直接建立索引
        if (level < 0) level = BTree.defaultLevel;
        // 创建索引
        BTree bTree = new BTree(fieldName, level);
        // 将索引加入表
        indexes.add(bTree);
    }
}
