package com.example.jdbctool.app;

import com.example.jdbctool.entity.College;
import com.example.jdbctool.entity.Student;
import com.example.jdbctool.util.DBUtil;
import com.example.jdbctool.util.JDBCTool;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

/**
 * 演示程序：使用 JDBCTool 对 college（学院表）与 student（学生表）完成增删改查。
 *
 * @author homework
 */
public class Main {

    private static final String LINE = "============================================================";
    private static final String SUB = "------------------------------------------------------------";

    public static void main(String[] args) throws Exception {
        System.out.println(LINE);
        System.out.println("   ORM + Java 反射 + JDBC   —— 通用工具类 JDBCTool 演示");
        System.out.println(LINE);

        // ========== 0. 初始化数据库（建库 DateTest + 建表） ==========
        DBUtil.ensureDatabaseAndTables();
        System.out.println("[初始化] 数据库 DateTest 与数据表 student / college 已就绪。");

        try (Connection connection = DBUtil.getConnection()) {
            System.out.println("[连接成功] " + connection.getMetaData().getURL());
            System.out.println();

            // ========== 1. 学院表 CRUD ==========
            demoCollege(connection);
            System.out.println();

            // ========== 2. 学生表 CRUD ==========
            demoStudent(connection);
        }

        System.out.println();
        System.out.println(LINE);
        System.out.println("   全部 CRUD 操作演示完成，运行成功！");
        System.out.println(LINE);
    }

    /* ================================================================== */
    /* ========                学院表 college                   ======== */
    /* ================================================================== */

    private static void demoCollege(Connection connection) throws Exception {
        System.out.println("【一】学院表 college 的增删改查");
        System.out.println(SUB);

        String[] ids = {"C001", "C002", "C003", "C004"};
        cleanCollege(connection, ids);

        // ---- 新增（save） ----
        System.out.println("① 新增 save()：向 college 表插入 3 条记录");
        int a = JDBCTool.save(new College("C001", "计算机学院", "CS001"), connection);
        int b = JDBCTool.save(new College("C002", "软件学院", "SE002"), connection);
        int c = JDBCTool.save(new College("C003", "外国语学院", "FL003"), connection);
        System.out.println("   受影响行数 = " + (a + b + c) + "  （每条新增返回 1）");
        System.out.println();

        // ---- 查询全部（resultSetToList） ----
        System.out.println("② 查询全部 resultSetToList()：");
        for (College college : queryAll(connection, College.class, JDBCTool.getTableName(College.class))) {
            System.out.println("   " + college);
        }
        System.out.println();

        // ---- 按主键查询（getOneById） ----
        System.out.println("③ 按主键查询 getOneById(\"C002\")：");
        System.out.println("   " + JDBCTool.getOneById("C002", College.class, connection));
        System.out.println();

        // ---- 更新（update） ----
        System.out.println("④ 更新 update()：把 C002 的学院代码改为 SE-2024");
        College update = new College("C002", "软件学院", "SE-2024");
        System.out.println("   受影响行数 = " + JDBCTool.update(update, connection));
        System.out.println("   更新后 = " + JDBCTool.getOneById("C002", College.class, connection));
        System.out.println();

        // ---- 删除（delete） ----
        System.out.println("⑤ 删除 delete()：删除 C003");
        College delete = new College();
        delete.setId("C003");
        System.out.println("   受影响行数 = " + JDBCTool.delete(delete, connection));
        System.out.println();

        // ---- 删除后复查 ----
        System.out.println("⑥ 删除后最终列表：");
        for (College college : queryAll(connection, College.class, JDBCTool.getTableName(College.class))) {
            System.out.println("   " + college);
        }
        System.out.println("（可见 C003 已被删除，C002 的代码已被更新）");
    }

    /* ================================================================== */
    /* ========                学生表 student                   ======== */
    /* ================================================================== */

    private static void demoStudent(Connection connection) throws Exception {
        System.out.println("【二】学生表 student 的增删改查");
        System.out.println(SUB);

        String[] ids = {"S2024001", "S2024002", "S2024003", "S2024004"};
        cleanStudent(connection, ids);

        // ---- 新增（save） ----
        System.out.println("① 新增 save()：向 student 表插入 4 条记录");
        JDBCTool.save(new Student("S2024001", "张伟", "计算机科学与技术", 19,
                LocalDate.of(2024, 9, 1), false, new BigDecimal("5800.00")), connection);
        JDBCTool.save(new Student("S2024002", "李娜", "软件工程", 20,
                LocalDate.of(2023, 9, 1), false, new BigDecimal("6200.00")), connection);
        JDBCTool.save(new Student("S2024003", "王强", "人工智能", 21,
                LocalDate.of(2022, 9, 1), true, new BigDecimal("6800.00")), connection);
        JDBCTool.save(new Student("S2024004", "赵敏", "网络工程", 22,
                LocalDate.of(2021, 9, 1), true, new BigDecimal("5600.00")), connection);
        System.out.println("   已插入 4 条学生记录");
        System.out.println();

        // ---- 查询全部 ----
        System.out.println("② 查询全部 resultSetToList()：");
        for (Student student : queryAll(connection, Student.class, JDBCTool.getTableName(Student.class))) {
            System.out.println("   " + student);
        }
        System.out.println();

        // ---- 按主键查询 ----
        System.out.println("③ 按主键查询 getOneById(\"S2024003\")：");
        System.out.println("   " + JDBCTool.getOneById("S2024003", Student.class, connection));
        System.out.println();

        // ---- 更新 ----
        System.out.println("④ 更新 update()：S2024002 更名为「李娜娜」，年龄 21，学费调整为 6500.00");
        Student update = new Student("S2024002", "李娜娜", "软件工程", 21,
                LocalDate.of(2023, 9, 1), false, new BigDecimal("6500.00"));
        System.out.println("   受影响行数 = " + JDBCTool.update(update, connection));
        System.out.println("   更新后 = " + JDBCTool.getOneById("S2024002", Student.class, connection));
        System.out.println();

        // ---- 删除 ----
        System.out.println("⑤ 删除 delete()：删除 S2024004");
        Student delete = new Student();
        delete.setId("S2024004");
        System.out.println("   受影响行数 = " + JDBCTool.delete(delete, connection));
        System.out.println();

        // ---- 删除后复查 ----
        System.out.println("⑥ 删除后最终列表：");
        for (Student student : queryAll(connection, Student.class, JDBCTool.getTableName(Student.class))) {
            System.out.println("   " + student);
        }
        System.out.println("（可见 S2024004 已被删除，S2024002 的信息已被更新）");
    }

    /* ========================= 私有辅助 ========================= */

    /**
     * 复用【方法1】：执行 {@code SELECT *} 并把结果集转换为对象列表。
     */
    private static <T> List<T> queryAll(Connection connection, Class<T> clazz, String tableName) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM " + tableName + " ORDER BY id")) {
            return JDBCTool.resultSetToList(rs, clazz);
        }
    }

    private static void cleanCollege(Connection connection, String[] ids) throws Exception {
        for (String id : ids) {
            College college = new College();
            college.setId(id);
            JDBCTool.delete(college, connection);
        }
    }

    private static void cleanStudent(Connection connection, String[] ids) throws Exception {
        for (String id : ids) {
            Student student = new Student();
            student.setId(id);
            JDBCTool.delete(student, connection);
        }
    }
}
