package com.chengyi.chendb;

import com.chengyi.chendb.data.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Test {
    static Table<Student> studentTable;

    public static void main(String[] args) {
        studentTable = new Table<>("student", 100);
        studentTable.createIndexOnField("id");

        int cnt = 5000;

        long startTime = System.currentTimeMillis();
        testSingleInsert();
        testMultiInsert(cnt);
        long endTime = System.currentTimeMillis();
        System.out.println("all data insert finished");
        System.out.println(String.format("插入 %d 条数据花费 %f s", cnt, (endTime - startTime) / 1000.0));
    }

    public static void testSingleInsert() {
        Student chengyi = new Student(20, "chengyi", "S18071228");
        studentTable.insertRecord(chengyi);
    }

    public static void testMultiInsert(int cnt) {
        List<Student> students = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < cnt; i++) {
            int id = random.nextInt(cnt);
            Student stu = new Student(id, geneRandomStr(10), geneRandomStr(10));
            students.add(stu);
        }

        // TODO 暂时采用单次调用形式
        for (Student student : students) {
            studentTable.insertRecord(student);
        }
    }

    public static String geneRandomStr(int len) {
        String base = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(base.charAt(random.nextInt(base.length())));
        }
        return sb.toString();
    }

}
