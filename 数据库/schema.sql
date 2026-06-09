-- ============================================================
-- 房产管理系统 - Spring Boot 自动建表脚本
-- ============================================================

CREATE TABLE IF NOT EXISTS house_standard (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    area DECIMAL(10,2) NOT NULL UNIQUE COMMENT '住房面积（㎡）',
    min_score INT NOT NULL COMMENT '该面积对应的最低住房分数阈值'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='住房标准表';

CREATE TABLE IF NOT EXISTS house (
    house_no VARCHAR(50) PRIMARY KEY COMMENT '房号',
    area DECIMAL(10,2) NOT NULL COMMENT '住房面积（㎡）',
    status INT NOT NULL COMMENT '分配标志：0-空闲, 1-已分配',
    rent_per_sqm DECIMAL(10,2) NOT NULL COMMENT '每平方米房租（元/㎡）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房产表';

CREATE TABLE IF NOT EXISTS resident (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    owner_name VARCHAR(50) NOT NULL COMMENT '户主姓名',
    department VARCHAR(100) NOT NULL COMMENT '所属部门',
    title VARCHAR(50) NOT NULL COMMENT '职称',
    family_size INT NOT NULL COMMENT '家庭人口',
    score INT NOT NULL COMMENT '住房分数',
    house_no VARCHAR(50) NOT NULL UNIQUE COMMENT '房号（唯一约束，一户一房）',
    house_area DECIMAL(10,2) NOT NULL COMMENT '住房面积',
    FOREIGN KEY (house_no) REFERENCES house(house_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='住户表';

CREATE TABLE IF NOT EXISTS rent_bill (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    house_no VARCHAR(50) NOT NULL COMMENT '房号',
    owner_name VARCHAR(50) NOT NULL COMMENT '户主姓名',
    bill_month VARCHAR(10) NOT NULL COMMENT '账单月份',
    rent_amount DECIMAL(10,2) NOT NULL COMMENT '房租金额（元）',
    create_time DATE COMMENT '创建时间',
    FOREIGN KEY (house_no) REFERENCES house(house_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房租表';

CREATE TABLE IF NOT EXISTS application (
    apply_no VARCHAR(50) PRIMARY KEY COMMENT '申请单编号',
    applicant_name VARCHAR(50) NOT NULL COMMENT '申请人姓名',
    department VARCHAR(100) NOT NULL COMMENT '部门',
    apply_type INT NOT NULL COMMENT '申请类型：1-分房, 2-调房, 3-退房',
    apply_status INT NOT NULL COMMENT '处理状态：0-待处理, 1-已通过, 2-已驳回',
    req_area DECIMAL(10,2) COMMENT '要求住房面积',
    old_house_no VARCHAR(50) COMMENT '原房号',
    allocated_house_no VARCHAR(50) COMMENT '分配房号',
    title VARCHAR(50) COMMENT '职称',
    family_size INT COMMENT '家庭人口',
    score INT COMMENT '住房分数',
    old_house_area DECIMAL(10,2) COMMENT '原住房面积',
    reject_reason VARCHAR(500) COMMENT '驳回原因',
    create_time DATETIME COMMENT '创建时间',
    process_time DATETIME COMMENT '处理时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申请表';
