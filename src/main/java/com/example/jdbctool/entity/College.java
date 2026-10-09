package com.example.jdbctool.entity;

import com.example.jdbctool.annotation.Column;
import com.example.jdbctool.annotation.Table;

/**
 * 学院实体类 —— 对应数据库表 {@code college}。
 * <p>
 * 表结构：
 * <pre>
 * id    varchar 主键  学院编号
 * name  varchar       学院名称
 * code  varchar       学院代码
 * </pre>
 *
 * @author homework
 */
@Table("college")
public class College {

    /** 学院编号（主键） */
    @Column(value = "id", id = true)
    private String id;

    /** 学院名称 */
    @Column("name")
    private String name;

    /** 学院代码 */
    @Column("code")
    private String code;

    /* ==================== 构造方法 ==================== */

    public College() {
    }

    public College(String id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    /* ==================== toString ==================== */

    @Override
    public String toString() {
        return String.format("College{学院编号='%s', 学院名称='%s', 学院代码='%s'}", id, name, code);
    }
}
