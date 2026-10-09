package com.example.jdbctool.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段（列）映射注解。
 * <p>
 * 标注在实体类的属性上，用于告诉 JDBCTool 该属性对应表中的哪个列，
 * 以及该属性是否为主键。
 * <pre>
 *     &#64;Column("enroll_date")          // 普通列
 *     private LocalDate enrollDate;
 *
 *     &#64;Column(value = "id", id = true) // 主键列
 *     private String id;
 * </pre>
 *
 * @author homework
 */
@Retention(RetentionPolicy.RUNTIME)     // 运行期保留 —— 反射才能读到
@Target(ElementType.FIELD)              // 只能标注在"属性"上
public @interface Column {

    /** 数据库列名 */
    String value();

    /** 该列是否为主键，默认 false */
    boolean id() default false;
}
