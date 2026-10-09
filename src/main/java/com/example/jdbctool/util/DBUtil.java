package com.example.jdbctool.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

/**
 * 数据库连接工具类。
 * <p>
 * 负责读取 {@code db.properties} 配置、注册驱动、提供 Connection，
 * 并支持「一键初始化数据库与数据表」。
 *
 * @author homework
 */
public final class DBUtil {

    /** 配置文件中的配置项 */
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("找不到 db.properties 配置文件");
            }
            PROPS.load(in);
            // 注册 JDBC 驱动（MySQL 8 之后可省略，这里显式加载便于理解）
            Class.forName(PROPS.getProperty("jdbc.driver"));
        } catch (Exception e) {
            throw new ExceptionInInitializerError("初始化数据库配置失败：" + e.getMessage());
        }
    }

    private DBUtil() {
    }

    /**
     * 获取一个指向业务库 DateTest 的连接。
     */
    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(
                PROPS.getProperty("jdbc.url"),
                PROPS.getProperty("jdbc.username"),
                PROPS.getProperty("jdbc.password"));
    }

    /**
     * 一键初始化：创建数据库 DateTest 与两张业务表（表已存在则跳过）。
     * <p>
     * 这样即使换一台机器，只要 MySQL 账号密码正确，直接运行主程序即可自动建库建表。
     */
    public static void ensureDatabaseAndTables() throws Exception {
        String baseUrl = PROPS.getProperty("jdbc.baseUrl");
        String username = PROPS.getProperty("jdbc.username");
        String password = PROPS.getProperty("jdbc.password");

        // 1. 连到 MySQL 服务器（不指定库），创建数据库
        try (Connection conn = DriverManager.getConnection(baseUrl, username, password);
             Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS DateTest "
                    + "DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
        }

        // 2. 连到 DateTest 库，创建两张表
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS college ("
                    + "  id   VARCHAR(32) NOT NULL COMMENT '学院编号',"
                    + "  name VARCHAR(64)          COMMENT '学院名称',"
                    + "  code VARCHAR(32)          COMMENT '学院代码',"
                    + "  PRIMARY KEY (id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学院表'");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS student ("
                    + "  id          VARCHAR(32)  NOT NULL COMMENT '学号',"
                    + "  name        VARCHAR(64)           COMMENT '姓名',"
                    + "  major       VARCHAR(64)           COMMENT '专业',"
                    + "  age         INT                   COMMENT '年龄',"
                    + "  enroll_date DATE                  COMMENT '入学时间',"
                    + "  graduated   TINYINT(1)            COMMENT '是否毕业 0否1是',"
                    + "  tuition     DECIMAL(10,2)         COMMENT '学费',"
                    + "  PRIMARY KEY (id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表'");
        }
    }
}
