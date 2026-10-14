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
    note_id BIGINT NOT NULL COMMENT '所属记录ID，应用层逻辑关联',
    original_name VARCHAR(255) NOT NULL COMMENT '上传时的图片文件名',
    storage_key VARCHAR(100) NOT NULL COMMENT 'OSS对象Key',
    mime_type VARCHAR(50) NOT NULL COMMENT '图片媒体类型',
    size_bytes BIGINT NOT NULL COMMENT '图片大小，单位字节',
    sort_order INT NOT NULL COMMENT '相册显示顺序',
    create_time DATETIME(3) NOT NULL COMMENT '上传时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_note_image_name (note_id, original_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记录图片';

-- 本地联调用示例记录；重复执行时按文件名跳过，避免重复插入。
INSERT INTO note (title, md_name, content, category, create_time)
SELECT '让想法慢慢长出来', 'demo-life-weekend.md', '## 周末的小事\n\n今天没有急着把清单划完。去街角买了花，回来给书桌腾出一点空地。\n\n原来日子不需要每一刻都被安排，留白也可以是一种认真。', 'LIFE', '2026-10-11 20:18:00.000'
WHERE NOT EXISTS (SELECT 1 FROM note WHERE md_name = 'demo-life-weekend.md');

INSERT INTO note (title, md_name, content, category, create_time)
SELECT '一段新项目的开始', 'demo-work-project.md', '## 今日进展\n\n整理了需求与接口边界。先把最基础的列表、详情和上传流程跑通，再逐步补齐体验。\n\n- 明确工作与生活两类记录\n- 用 Markdown 留下完整上下文\n- 保持页面简单、安静', 'WORK', '2026-10-09 16:42:00.000'
WHERE NOT EXISTS (SELECT 1 FROM note WHERE md_name = 'demo-work-project.md');

INSERT INTO note (title, md_name, content, category, create_time, update_time)
SELECT '下班路上的晚霞', 'demo-life-sunset.md', '路过桥边时，天空正好变成了淡淡的橘色。\n\n> 有些值得记住的瞬间，发生时并没有声音。\n\n以后想多给自己一点停下来看的时间。', 'LIFE', '2026-10-07 18:35:00.000', '2026-10-08 09:12:00.000'
WHERE NOT EXISTS (SELECT 1 FROM note WHERE md_name = 'demo-life-sunset.md');

INSERT INTO note (title, md_name, content, category, create_time)
SELECT '把复杂的问题拆成小步', 'demo-work-review.md', '本周最有效的方法，是先画清楚数据从哪里来、到哪里去。\n\n遇到陌生代码时，先找入口、再找状态、最后看异常路径。小步验证，比一次写完更踏实。', 'WORK', '2026-10-03 10:05:00.000'
WHERE NOT EXISTS (SELECT 1 FROM note WHERE md_name = 'demo-work-review.md');
