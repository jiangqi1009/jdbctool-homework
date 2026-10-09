# JDBCTool —— 基于 Java 反射 + JDBC 的简易 ORM 工具

> 课程作业：理解 ORM 原理、掌握 Java 反射机制、熟练使用 JDBC 完成增删改查。

## 一、项目简介

本项目实现了一个通用数据库工具类 `JDBCTool`。它通过**运行时反射**读取实体类上的
`@Table` / `@Column` 注解，动态推导表名、列名与主键，从而生成通用的
`INSERT / UPDATE / DELETE / SELECT` 语句。整个工具类不依赖任何具体实体，
换一个 `@Table` 实体即可复用，这正是 ORM（对象关系映射）框架的最小实现思路。

## 二、技术栈

| 项目 | 说明 |
| --- | --- |
| JDK | 17 |
| 构建工具 | Maven 3.9 |
| 数据库 | MySQL 8.0（数据库名 `DateTest`） |
| 驱动 | `com.mysql:mysql-connector-j:8.0.33` |
| 核心技术 | 自定义注解 + 反射 + 泛型 + JDBC PreparedStatement |

## 三、目录结构

```
jdbctool-homework
├── pom.xml                                  Maven 构建文件
├── sql/init.sql                             建库建表脚本
├── src/main/resources/db.properties         数据库连接配置
└── src/main/java/com/example/jdbctool
    ├── annotation
    │   ├── Table.java                       表映射注解 @Table("student")
    │   └── Column.java                      列映射注解 @Column("name") / @Column(id = true)
    ├── entity
    │   ├── College.java                     学院实体（id, name, code）
    │   └── Student.java                     学生实体（id, name, major, age, 入学时间, 是否毕业, 学费）
    ├── util
    │   ├── DBUtil.java                      连接 + 一键建库建表
    │   └── JDBCTool.java                    ★ 核心工具类（5 个方法 + 辅助方法）
    └── app
        └── Main.java                        演示程序（两张表的增删改查）
```

## 四、数据库设计（DateTest）

```sql
CREATE DATABASE DateTest;

-- 学院表
CREATE TABLE college (
    id   VARCHAR(32) NOT NULL PRIMARY KEY COMMENT '学院编号',
    name VARCHAR(64) COMMENT '学院名称',
    code VARCHAR(32) COMMENT '学院代码'
);

-- 学生表
CREATE TABLE student (
    id          VARCHAR(32) NOT NULL PRIMARY KEY COMMENT '学号',
    name        VARCHAR(64)  COMMENT '姓名',
    major       VARCHAR(64)  COMMENT '专业',
    age         INT          COMMENT '年龄',
    enroll_date DATE         COMMENT '入学时间',
    graduated   TINYINT(1)   COMMENT '是否毕业 0否1是',
    tuition     DECIMAL(10,2) COMMENT '学费'
);
```

## 五、核心 API

| 方法 | 作用 |
| --- | --- |
| `<T> List<T> resultSetToList(ResultSet rs, Class<T> clazz)` | 结果集 → 对象列表（查询） |
| `<T> int save(T obj, Connection connection)` | 新增 |
| `<T> int update(T obj, Connection connection)` | 按主键更新 |
| `<T> int delete(T obj, Connection connection)` | 按主键删除 |
| `<T> T getOneById(String id, Class<T> clazz, Connection connection)` | 按主键查询单条 |

辅助方法：`getTableName`、`resolveColumnName`、`getColumnMappings`、`getPrimaryKeyField`、
`getAllFields`、`findFieldByColumnName`、`readFieldValue`、`newInstance`、
`convertValue`、`toSqlParameter`、`executeUpdate`、`close`。

## 六、运行步骤

1. 确认 MySQL 已启动，并修改 `src/main/resources/db.properties` 中的账号密码。
2. （可选）执行 `sql/init.sql` 手动建库建表；不执行也可以，主程序会自动建库建表。
3. 编译打包：

   ```bash
   mvn clean package
   ```

4. 运行：

   ```bash
   java -jar target/jdbctool-homework.jar
   ```

   或

   ```bash
   mvn exec:java -Dexec.mainClass=com.example.jdbctool.app.Main
   ```

## 七、Git 版本管理

```bash
git init
git add .
git commit -m "feat: 完成 JDBCTool 工具类与两张表的增删改查演示"
```

## 八、原理说明

1. **ORM 映射**：类 ↔ 表、属性 ↔ 列，通过 `@Table` / `@Column` 描述映射关系。
2. **反射创建对象**：`clazz.getDeclaredConstructor().newInstance()` 根据 Class 动态实例化。
3. **反射赋值**：`field.setAccessible(true)` 后 `field.set(obj, value)` 为私有属性赋值。
4. **列名匹配**：用 `ResultSetMetaData` 拿到列名，再用 `findFieldByColumnName` 反查属性。
5. **动态 SQL**：遍历反射得到的「列名—值」映射，拼出 `INSERT/UPDATE/DELETE` 的占位符语句。
6. **类型转换**：`convertValue` 负责把 `DATE / TINYINT / DECIMAL` 等 JDBC 类型转换成 `LocalDate / Boolean / BigDecimal`。
7. **防注入**：所有值统一走 `PreparedStatement` 的 `?` 占位符绑定。
