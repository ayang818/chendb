package com.chengyi.chendb.idx;

public interface Index {

    /**
     * 树中插入或原地更新
     * @param key
     * @param pointer
     */
    void insertOrUpdate(Comparable key, Object pointer);

    /**
     * 
     * @param key
     * @return
     */
    Object get(Object key);

    void remove(Object key);

    /**
     * 获取索引对应字段名称
     * @return
     */
    String getKeyName();
}
