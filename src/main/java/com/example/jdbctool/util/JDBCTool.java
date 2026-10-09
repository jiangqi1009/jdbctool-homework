package com.example.jdbctool.util;

import com.example.jdbctool.annotation.Column;
import com.example.jdbctool.annotation.Table;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC 通用工具类（一个基于<b>反射 + 泛型</b>的极简 ORM 实现）。
 *
 * <h3>设计思想</h3>
 * <ol>
 *   <li><b>约定优于配置</b>：实体类通过 {@link Table}、{@link Column} 注解声明「类 ↔ 表」「属性 ↔ 列」的映射关系；</li>
 *   <li><b>反射驱动</b>：运行时通过反射读取注解、遍历属性、创建对象、为属性赋值，从而写出与具体实体无关的通用代码；</li>
 *   <li><b>SQL 动态拼接 + PreparedStatement 预编译</b>：列名由反射推导，值一律使用 {@code ?} 占位符，防止 SQL 注入。</li>
 * </ol>
 *
 * <h3>提供的 5 个核心方法</h3>
 * <ol>
 *   <li>{@link #resultSetToList(ResultSet, Class)} —— ResultSet → 对象列表（查询）</li>
 *   <li>{@link #save(Object, Connection)}     —— 新增</li>
 *   <li>{@link #update(Object, Connection)}   —— 修改（按主键）</li>
 *   <li>{@link #delete(Object, Connection)}   —— 删除（按主键）</li>
 *   <li>{@link #getOneById(String, Class, Connection)} —— 按主键查询单条</li>
 * </ol>
 *
 * @author homework
 */
public final class JDBCTool {

    /** 工具类禁止实例化 */
    private JDBCTool() {
    }

    /* ==================================================================== */
    /* ========================= 一、5 个核心方法 ========================== */
    /* ==================================================================== */

    /**
     * 【方法1】把 {@link ResultSet} 结果集转换为实体对象列表。
     * <p>
     * 核心步骤：
     * <ol>
     *   <li>通过 {@link ResultSetMetaData} 取得每一列的列名（别名）；</li>
     *   <li>遍历结果集的每一行，用反射 {@code clazz.getDeclaredConstructor().newInstance()} 创建空对象；</li>
     *   <li>对每一列，找到实体中与列名对应的属性，反射赋值（并做类型转换），最终组成 List 返回。</li>
     * </ol>
     *
     * @param rs    已执行查询、位于首行之前的 ResultSet
     * @param clazz 目标实体类的 Class 对象
     * @param <T>   实体类型
     * @return 实体对象列表；若结果集无数据则返回空 List（不会返回 null）
     */
    public static <T> List<T> resultSetToList(ResultSet rs, Class<T> clazz) throws Exception {
        List<T> list = new ArrayList<>();
        if (rs == null) {
            return list;
        }
        // 1. 取得结果集的"元数据"，从中拿到列的数量与列名
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // 2. 逐行遍历
        while (rs.next()) {
            // 2.1 反射创建实例（要求实体类有无参构造方法）
            T obj = newInstance(clazz);

            // 2.2 逐列填值
            for (int i = 1; i <= columnCount; i++) {   // JDBC 列下标从 1 开始
                String columnLabel = metaData.getColumnLabel(i); // 列的别名/名称
                Object columnValue = rs.getObject(i);            // 列的值

                if (columnValue == null) {
                    continue;                                    // 值为 null 时跳过（保持属性默认值）
                }
                Field field = findFieldByColumnName(clazz, columnLabel);
                if (field == null) {
                    continue;                                    // 该列在实体中没有对应属性，忽略
                }
                field.setAccessible(true);                       // 允许访问 private 属性
                field.set(obj, convertValue(columnValue, field.getType())); // 类型转换后赋值
            }
            list.add(obj);
        }
        return list;
    }

    /**
     * 【方法2】保存（新增）一个对象。
     * <p>
     * 生成的 SQL 形如：
     * <pre>INSERT INTO student (id, name, major, ...) VALUES (?, ?, ?, ...)</pre>
     *
     * @param obj        待保存的实体对象（其 Class 上必须有 {@link Table} 注解）
     * @param connection 数据库连接
     * @return 受影响的行数（成功为 1）
     */
    public static <T> int save(T obj, Connection connection) throws Exception {
        Class<?> clazz = obj.getClass();
        String tableName = getTableName(clazz);
        Map<Field, String> columns = getColumnMappings(clazz);

        StringBuilder columnSql = new StringBuilder();   // 列名部分
        StringBuilder placeholderSql = new StringBuilder(); // ? 占位符部分
        List<Object> values = new ArrayList<>();          // 与占位符一一对应的值

        for (Map.Entry<Field, String> entry : columns.entrySet()) {
            Field field = entry.getKey();
            String columnName = entry.getValue();
            columnSql.append(columnName).append(", ");
            placeholderSql.append("?, ");
            values.add(readFieldValue(field, obj));
        }
        // 去掉末尾多余的 ", "
        trimTail(columnSql);
        trimTail(placeholderSql);

        String sql = "INSERT INTO " + tableName
                + " (" + columnSql + ") VALUES (" + placeholderSql + ")";
        return executeUpdate(connection, sql, values);
    }

    /**
     * 【方法3】根据主键更新一个对象。
     * <p>
     * 生成的 SQL 形如：
     * <pre>UPDATE student SET name=?, major=?, ... WHERE id=?</pre>
     * 注意：SET 子句中<b>不包含</b>主键本身。
     *
     * @param obj        待更新的实体对象（主键属性必须已赋值）
     * @param connection 数据库连接
     * @return 受影响的行数（成功为 1，主键不存在为 0）
     */
    public static <T> int update(T obj, Connection connection) throws Exception {
        Class<?> clazz = obj.getClass();
        String tableName = getTableName(clazz);
        Field idField = getPrimaryKeyField(clazz);
        if (idField == null) {
            throw new IllegalStateException("实体类 " + clazz.getName()
                    + " 未声明主键（请在属性上使用 @Column(id = true) 标注）");
        }

        Map<Field, String> columns = getColumnMappings(clazz);
        StringBuilder setSql = new StringBuilder();
        List<Object> values = new ArrayList<>();

        for (Map.Entry<Field, String> entry : columns.entrySet()) {
            Field field = entry.getKey();
            if (field.equals(idField)) {
                continue;                       // 主键不参与 SET
            }
            setSql.append(entry.getValue()).append(" = ?, ");
            values.add(readFieldValue(field, obj));
        }
        trimTail(setSql);

        String idColumn = resolveColumnName(idField);
        String sql = "UPDATE " + tableName + " SET " + setSql + " WHERE " + idColumn + " = ?";
        values.add(readFieldValue(idField, obj));  // WHERE 条件的值放在最后

        return executeUpdate(connection, sql, values);
    }

    /**
     * 【方法4】根据主键删除一个对象。
     * <p>
     * 生成的 SQL 形如：
     * <pre>DELETE FROM student WHERE id = ?</pre>
     *
     * @param obj        待删除的实体对象（只需给主键属性赋值即可）
     * @param connection 数据库连接
     * @return 受影响的行数（成功为 1，主键不存在为 0）
     */
    public static <T> int delete(T obj, Connection connection) throws Exception {
        Class<?> clazz = obj.getClass();
        String tableName = getTableName(clazz);
        Field idField = getPrimaryKeyField(clazz);
        if (idField == null) {
            throw new IllegalStateException("实体类 " + clazz.getName() + " 未声明主键");
        }
        String sql = "DELETE FROM " + tableName + " WHERE " + resolveColumnName(idField) + " = ?";
        List<Object> values = new ArrayList<>();
        values.add(readFieldValue(idField, obj));
        return executeUpdate(connection, sql, values);
    }

    /**
     * 【方法5】根据主键查询一个对象。
     * <p>
     * 生成的 SQL 形如：
     * <pre>SELECT * FROM student WHERE id = ?</pre>
     *
     * @param id         主键值
     * @param clazz      目标实体类的 Class 对象
     * @param connection 数据库连接
     * @param <T>        实体类型
     * @return 查询到的对象；若不存在则返回 {@code null}
     */
    public static <T> T getOneById(String id, Class<T> clazz, Connection connection) throws Exception {
        String tableName = getTableName(clazz);
        Field idField = getPrimaryKeyField(clazz);
        if (idField == null) {
            throw new IllegalStateException("实体类 " + clazz.getName() + " 未声明主键");
        }
        String sql = "SELECT * FROM " + tableName + " WHERE " + resolveColumnName(idField) + " = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                // 复用【方法1】把结果集转成对象列表，取第一条
                List<T> list = resultSetToList(rs, clazz);
                return list.isEmpty() ? null : list.get(0);
            }
        }
    }

    /* ==================================================================== */
    /* ====================== 二、辅助方法（统一访问底座） ================== */
    /* ==================================================================== */

    /**
     * 辅助方法：执行增删改（INSERT / UPDATE / DELETE）的统一入口。
     * <p>
     * 负责创建 {@link PreparedStatement}、按顺序绑定参数、执行并返回受影响行数，
     * 并自动关闭 Statement（try-with-resources）。Connection 由调用方管理，不在此关闭。
     */
    private static int executeUpdate(Connection connection, String sql, List<Object> values) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, toSqlParameter(values.get(i)));
            }
            return ps.executeUpdate();
        }
    }

    /**
     * 辅助方法：解析实体类对应的表名。
     * 优先取 {@link Table} 注解的值，未标注时退化为「类名小写」。
     */
    public static String getTableName(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (table != null && !table.value().isEmpty()) {
            return table.value();
        }
        return clazz.getSimpleName().toLowerCase();
    }

    /**
     * 辅助方法：解析某个属性对应的列名。
     * 优先取 {@link Column} 注解的值，未标注时退化为「属性名」。
     */
    public static String resolveColumnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.value().isEmpty()) {
            return column.value();
        }
        return field.getName();
    }

    /**
     * 辅助方法：取得实体类全部「需要参与 SQL」的属性，并解析出对应的列名。
     * 使用 {@link LinkedHashMap} 保证「属性顺序 = 列顺序」，从而与占位符顺序严格一致。
     *
     * @return 有序的 Map：key 为 Field，value 为列名
     */
    public static Map<Field, String> getColumnMappings(Class<?> clazz) {
        Map<Field, String> mappings = new LinkedHashMap<>();
        for (Field field : getAllFields(clazz)) {
            mappings.put(field, resolveColumnName(field));
        }
        return mappings;
    }

    /**
     * 辅助方法：取得实体类的主键属性。
     * 优先找 {@code @Column(id = true)} 标注的属性；找不到再退化为名为 "id" 的属性。
     *
     * @return 主键 Field；没有则返回 null
     */
    public static Field getPrimaryKeyField(Class<?> clazz) {
        Field fallback = null;
        for (Field field : getAllFields(clazz)) {
            Column column = field.getAnnotation(Column.class);
            if (column != null && column.id()) {
                return field;
            }
            if ("id".equalsIgnoreCase(field.getName())) {
                fallback = field;
            }
        }
        return fallback;
    }

    /**
     * 辅助方法：反射创建实例（要求存在无参构造方法）。
     */
    public static <T> T newInstance(Class<T> clazz) throws Exception {
        Constructor<T> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    /**
     * 辅助方法：获取一个类（含其父类）的所有非静态、非合成属性。
     * 支持实体继承场景。
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                    continue;   // 静态 / 瞬态 / 编译器生成的属性不参与映射
                }
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * 辅助方法：根据「列名」在实体类中找到对应的属性（大小写不敏感）。
     * 先按 {@link Column} 注解的列名匹配，再退化为按属性名匹配。
     */
    public static Field findFieldByColumnName(Class<?> clazz, String columnName) {
        for (Field field : getAllFields(clazz)) {
            if (resolveColumnName(field).equalsIgnoreCase(columnName)) {
                return field;
            }
        }
        // 兜底：按属性名匹配
        for (Field field : getAllFields(clazz)) {
            if (field.getName().equalsIgnoreCase(columnName)) {
                return field;
            }
        }
        return null;
    }

    /**
     * 辅助方法：反射读取属性值（自动 setAccessible）。
     */
    public static Object readFieldValue(Field field, Object target) throws IllegalAccessException {
        field.setAccessible(true);
        return field.get(target);
    }

    /**
     * 辅助方法：把 ResultSet 中的值转换成实体属性的目标类型。
     * <p>
     * 覆盖了作业中用到的全部类型：String、Integer、Long、Double、Boolean、
     * BigDecimal、LocalDate、LocalDateTime、java.sql.Date / Timestamp 等。
     */
    public static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        // 类型已经兼容，直接返回
        if (targetType.isInstance(value)) {
            return value;
        }
        // 基本类型 → 包装类型（用于后续判断）
        Class<?> type = wrap(targetType);

        if (type == String.class) {
            return value.toString();
        }
        if (type == Integer.class) {
            return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString().trim());
        }
        if (type == Long.class) {
            return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(value.toString().trim());
        }
        if (type == Short.class) {
            return value instanceof Number ? ((Number) value).shortValue() : Short.parseShort(value.toString().trim());
        }
        if (type == Byte.class) {
            return value instanceof Number ? ((Number) value).byteValue() : Byte.parseByte(value.toString().trim());
        }
        if (type == Double.class) {
            return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(value.toString().trim());
        }
        if (type == Float.class) {
            return value instanceof Number ? ((Number) value).floatValue() : Float.parseFloat(value.toString().trim());
        }
        if (type == Boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            if (value instanceof Number) {          // tinyint(1)：0/1
                return ((Number) value).intValue() != 0;
            }
            return Boolean.parseBoolean(value.toString().trim());
        }
        if (type == BigDecimal.class) {
            return value instanceof BigDecimal ? value : new BigDecimal(value.toString().trim());
        }
        if (type == LocalDate.class) {
            if (value instanceof java.sql.Date) {
                return ((java.sql.Date) value).toLocalDate();
            }
            if (value instanceof Timestamp) {
                return ((Timestamp) value).toLocalDateTime().toLocalDate();
            }
            return LocalDate.parse(value.toString().trim());
        }
        if (type == LocalDateTime.class) {
            if (value instanceof Timestamp) {
                return ((Timestamp) value).toLocalDateTime();
            }
            return LocalDateTime.parse(value.toString().trim().replace(' ', 'T'));
        }
        if (type == java.sql.Date.class) {
            if (value instanceof java.sql.Date) {
                return value;
            }
            if (value instanceof java.util.Date) {
                return new java.sql.Date(((java.util.Date) value).getTime());
            }
            return java.sql.Date.valueOf(LocalDate.parse(value.toString().trim()));
        }
        if (type == java.sql.Timestamp.class) {
            if (value instanceof Timestamp) {
                return value;
            }
            if (value instanceof java.util.Date) {
                return new Timestamp(((java.util.Date) value).getTime());
            }
            return Timestamp.valueOf(value.toString().trim());
        }
        if (type == java.util.Date.class) {
            if (value instanceof java.util.Date) {
                return value;
            }
            return java.sql.Timestamp.valueOf(value.toString().trim());
        }
        // 其它类型：交给 JDBC 驱动自己处理
        return value;
    }

    /**
     * 辅助方法：把 Java 对象转换成 JDBC 能直接 setObject 的类型。
     * （主要是把 java.time 类型转换为 java.sql 类型）
     */
    public static Object toSqlParameter(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate) {
            return java.sql.Date.valueOf((LocalDate) value);
        }
        if (value instanceof LocalDateTime) {
            return Timestamp.valueOf((LocalDateTime) value);
        }
        return value;
    }

    /**
     * 辅助方法：关闭 JDBC 资源（可变参数，容错处理）。
     * 建议在 try-with-resources 不便使用时调用。
     */
    public static void close(AutoCloseable... resources) {
        if (resources == null) {
            return;
        }
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception ignored) {
                    // 关闭失败无需向上抛出
                }
            }
        }
    }

    /* ========================= 私有小工具 ========================= */

    /** 去掉 StringBuilder 末尾的 ", " */
    private static void trimTail(StringBuilder sb) {
        int len = sb.length();
        if (len >= 2 && sb.charAt(len - 2) == ',' && sb.charAt(len - 1) == ' ') {
            sb.setLength(len - 2);
        }
    }

    /** 基本类型 → 包装类型 */
    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == boolean.class) return Boolean.class;
        if (type == char.class) return Character.class;
        return type;
    }
}
