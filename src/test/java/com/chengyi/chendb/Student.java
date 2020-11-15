package com.chengyi.chendb;

public class Student {
    int id;
    String name;
    String schoolId;

    public Student() {

    }

    public Student(int id, String name, String schoolId) {
        this.id = id;
        this.name = name;
        this.schoolId = schoolId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", schoolId='" + schoolId + '\'' +
                '}';
    }
}
