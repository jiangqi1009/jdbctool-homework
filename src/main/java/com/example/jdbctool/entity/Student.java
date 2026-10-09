package com.example.jdbctool.entity;

import com.example.jdbctool.annotation.Column;
import com.example.jdbctool.annotation.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 学生实体类 —— 对应数据库表 {@code student}。
 * <p>
 * 表结构：
 * <pre>
 * id          varchar  主键   学号
 * name        varchar        姓名
 * major       varchar        专业
 * age         int            年龄
 * enroll_date date           入学时间
 * graduated   tinyint(1)     是否毕业（0 否 / 1 是）
 * tuition     decimal        学费
 * </pre>
 *
 * @author homework
 */
@Table("student")
public class Student {

    /** 学号（主键） */
    @Column(value = "id", id = true)
    private String id;

    /** 姓名 */
    @Column("name")
    private String name;

    /** 专业 */
    @Column("major")
    private String major;

    /** 年龄 */
    @Column("age")
    private Integer age;

    /** 入学时间 */
    @Column("enroll_date")
    private LocalDate enrollDate;

    /** 是否毕业 */
    @Column("graduated")
    private Boolean graduated;

    /** 学费 */
    @Column("tuition")
    private BigDecimal tuition;

    /* ==================== 构造方法 ==================== */

    public Student() {
    }

    public Student(String id, String name, String major, Integer age,
                   LocalDate enrollDate, Boolean graduated, BigDecimal tuition) {
        this.id = id;
        this.name = name;
        this.major = major;
        this.age = age;
        this.enrollDate = enrollDate;
        this.graduated = graduated;
        this.tuition = tuition;
    }

    /* ==================== getter / setter ==================== */

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public LocalDate getEnrollDate() {
        return enrollDate;
    }

    public void setEnrollDate(LocalDate enrollDate) {
        this.enrollDate = enrollDate;
    }

    public Boolean getGraduated() {
        return graduated;
    }

    public void setGraduated(Boolean graduated) {
        this.graduated = graduated;
    }

    public BigDecimal getTuition() {
        return tuition;
    }

    public void setTuition(BigDecimal tuition) {
        this.tuition = tuition;
    }

    /* ==================== toString ==================== */

    @Override
    public String toString() {
        return String.format(
                "Student{学号='%s', 姓名='%s', 专业='%s', 年龄=%s, 入学时间=%s, 是否毕业=%s, 学费=%s}",
                id, name, major, age, enrollDate,
                Boolean.TRUE.equals(graduated) ? "是" : "否", tuition);
    }
}
