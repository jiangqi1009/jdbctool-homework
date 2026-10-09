-- =============================================================
--  作业数据库初始化脚本
--  数据库：DateTest
--  包含两张表：college（学院表）、student（学生表）
-- =============================================================

CREATE DATABASE IF NOT EXISTS DateTest
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE DateTest;

-- -------------------------------------------------------------
-- 学院表 college：id、name、code
-- -------------------------------------------------------------
DROP TABLE IF EXISTS college;
CREATE TABLE college (
    id   VARCHAR(32) NOT NULL COMMENT '学院编号',
    name VARCHAR(64)          COMMENT '学院名称',
    code VARCHAR(32)          COMMENT '学院代码',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学院表';

-- -------------------------------------------------------------
-- 学生表 student：id、name、major、age、入学时间、是否毕业、学费
-- -------------------------------------------------------------
DROP TABLE IF EXISTS student;
CREATE TABLE student (
    id          VARCHAR(32)  NOT NULL COMMENT '学号',
    name        VARCHAR(64)           COMMENT '姓名',
    major       VARCHAR(64)           COMMENT '专业',
    age         INT                   COMMENT '年龄',
    enroll_date DATE                  COMMENT '入学时间',
    graduated   TINYINT(1)            COMMENT '是否毕业 0否 1是',
    tuition     DECIMAL(10, 2)        COMMENT '学费',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生表';

-- -------------------------------------------------------------
-- 初始数据（可选）
-- -------------------------------------------------------------
INSERT INTO college (id, name, code) VALUES
    ('C001', '计算机学院', 'CS001'),
    ('C002', '软件学院',   'SE002'),
    ('C003', '外国语学院', 'FL003');

INSERT INTO student (id, name, major, age, enroll_date, graduated, tuition) VALUES
    ('S2024001', '张伟', '计算机科学与技术', 19, '2024-09-01', 0, 5800.00),
    ('S2024002', '李娜', '软件工程',         20, '2023-09-01', 0, 6200.00),
    ('S2024003', '王强', '人工智能',         21, '2022-09-01', 1, 6800.00),
    ('S2024004', '赵敏', '网络工程',         22, '2021-09-01', 1, 5600.00);
