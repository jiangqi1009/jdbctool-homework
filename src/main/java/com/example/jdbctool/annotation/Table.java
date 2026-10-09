package com.example.jdbctool.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表映射注解。
 * <p>
 * 标注在实体类上，用于告诉 JDBCTool 该实体对应数据库中的哪张表。
 * <pre>
 *     &#64;Table("student")
 *     public class Student { ... }
 * </pre>
 *
 * @author homework
 */
@Retention(RetentionPolicy.RUNTIME)   // 运行期保留 —— 反射才能读到
@Target(ElementType.TYPE)             // 只能标注在"类"上
public @interface Table {

    /** 数据库表名 */
    String value();
}
