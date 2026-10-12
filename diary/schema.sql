CREATE DATABASE IF NOT EXISTS diary CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE diary;

CREATE TABLE IF NOT EXISTS note (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    title VARCHAR(200) NULL COMMENT '自定义标题，为空时显示文档名',
    md_name VARCHAR(255) NOT NULL COMMENT '上传的Markdown文件名',
    content MEDIUMTEXT NOT NULL COMMENT 'Markdown原文',
    category VARCHAR(10) NOT NULL COMMENT '分类：WORK工作，LIFE生活',
    create_time DATETIME(3) NOT NULL COMMENT '首次保存时间',
    update_time DATETIME(3) NULL COMMENT '最近修改时间',
    delete_time DATETIME(3) NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    INDEX idx_note_time (delete_time, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时间线记录';

CREATE TABLE IF NOT EXISTS note_image (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    note_id BIGINT NOT NULL COMMENT '所属记录ID',
    original_name VARCHAR(255) NOT NULL COMMENT '上传时的图片文件名',
    storage_key VARCHAR(100) NOT NULL COMMENT '服务器内部存储文件名',
    mime_type VARCHAR(50) NOT NULL COMMENT '图片媒体类型',
    size_bytes BIGINT NOT NULL COMMENT '图片大小，单位字节',
    sort_order INT NOT NULL COMMENT '相册显示顺序',
    create_time DATETIME(3) NOT NULL COMMENT '上传时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_note_image_name (note_id, original_name),
    CONSTRAINT fk_note_image_note FOREIGN KEY (note_id) REFERENCES note (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记录图片';
