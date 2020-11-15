package com.chengyi.chendb.idx;

public interface Index {

    /**
     * 树中插入或原地更新
     * @param key
     * @param pointer
     */
    void insertOrUpdate(Comparable key, Object pointer);

    Object selectById(Comparable id);

    void remove(Comparable key);

    /**
     * 获取索引对应字段名称
     * @return
     */
    String getKeyName();
}
