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

        testSingleInsert();
        testMultiInsert();
    }

    public static void testSingleInsert() {
        Student chengyi = new Student(20, "chengyi", "S18071228");
        studentTable.insertRecord(chengyi);
    }

    public static void testMultiInsert() {
        List<Student> students = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            int id = random.nextInt(100);
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
