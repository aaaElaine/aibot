-- 扩展数据库表结构
-- 支持多知识库、文档管理、用户认证等功能

-- 1. 用户表（管理员）
DROP TABLE IF EXISTS sys_user CASCADE;
CREATE TABLE sys_user (
    id              BIGSERIAL                            PRIMARY KEY,
    username        VARCHAR(50)                          NOT NULL UNIQUE,
    password        VARCHAR(255)                         NOT NULL,
    nickname        VARCHAR(100),
    email           VARCHAR(100),
    phone           VARCHAR(20),
    role            VARCHAR(20)  DEFAULT 'ADMIN'         NOT NULL,
    status          VARCHAR(20)  DEFAULT 'ACTIVE'        NOT NULL,
    last_login_time TIMESTAMP,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    is_delete       BOOLEAN   DEFAULT FALSE              NOT NULL
);

CREATE INDEX idx_sys_user_username ON sys_user (username);
CREATE INDEX idx_sys_user_status ON sys_user (status) WHERE is_delete = FALSE;

COMMENT ON TABLE sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.role IS '角色：ADMIN-管理员, OPERATOR-操作员';
COMMENT ON COLUMN sys_user.status IS '状态：ACTIVE-启用, DISABLED-禁用';

-- 2. 知识库表
DROP TABLE IF EXISTS knowledge_base CASCADE;
CREATE TABLE knowledge_base (
    id              BIGSERIAL                            PRIMARY KEY,
    name            VARCHAR(200)                         NOT NULL,
    description     TEXT,
    icon            VARCHAR(100),
    status          VARCHAR(20)  DEFAULT 'ACTIVE'        NOT NULL,
    document_count  INTEGER     DEFAULT 0                NOT NULL,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    is_delete       BOOLEAN   DEFAULT FALSE              NOT NULL
);

CREATE INDEX idx_knowledge_base_status ON knowledge_base (status) WHERE is_delete = FALSE;
CREATE INDEX idx_knowledge_base_create_time ON knowledge_base (create_time DESC);

COMMENT ON TABLE knowledge_base IS '知识库表';
COMMENT ON COLUMN knowledge_base.status IS '状态：ACTIVE-启用, DISABLED-禁用';

-- 3. 文档分类表
DROP TABLE IF EXISTS document_category CASCADE;
CREATE TABLE document_category (
    id              BIGSERIAL                            PRIMARY KEY,
    kb_id           BIGINT                               NOT NULL,
    parent_id       BIGINT       DEFAULT 0               NOT NULL,
    name            VARCHAR(200)                         NOT NULL,
    sort_order      INTEGER      DEFAULT 0               NOT NULL,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    is_delete       BOOLEAN   DEFAULT FALSE              NOT NULL
);

CREATE INDEX idx_document_category_kb ON document_category (kb_id) WHERE is_delete = FALSE;
CREATE INDEX idx_document_category_parent ON document_category (parent_id);

COMMENT ON TABLE document_category IS '文档分类表';

-- 4. 文档表
DROP TABLE IF EXISTS document CASCADE;
CREATE TABLE document (
    id              BIGSERIAL                            PRIMARY KEY,
    kb_id           BIGINT                               NOT NULL,
    category_id     BIGINT,
    title           VARCHAR(500)                         NOT NULL,
    content         TEXT,
    file_type       VARCHAR(50),
    file_size       BIGINT,
    file_path       VARCHAR(500),
    vector_status   VARCHAR(20)  DEFAULT 'PENDING'       NOT NULL,
    quality_score   DECIMAL(5,2),
    version         INTEGER      DEFAULT 1               NOT NULL,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    is_delete       BOOLEAN   DEFAULT FALSE              NOT NULL
);

CREATE INDEX idx_document_kb ON document (kb_id) WHERE is_delete = FALSE;
CREATE INDEX idx_document_category ON document (category_id);
CREATE INDEX idx_document_vector_status ON document (vector_status);
CREATE INDEX idx_document_create_time ON document (create_time DESC);

COMMENT ON TABLE document IS '文档表';
COMMENT ON COLUMN document.vector_status IS '向量化状态：PENDING-待处理, PROCESSING-处理中, COMPLETED-已完成, FAILED-失败';

-- 5. 文档版本历史表
DROP TABLE IF EXISTS document_version CASCADE;
CREATE TABLE document_version (
    id              BIGSERIAL                            PRIMARY KEY,
    document_id     BIGINT                               NOT NULL,
    version         INTEGER                              NOT NULL,
    content         TEXT,
    file_path       VARCHAR(500),
    change_summary  VARCHAR(500),
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    is_delete       BOOLEAN   DEFAULT FALSE              NOT NULL
);

CREATE INDEX idx_document_version_doc ON document_version (document_id);
CREATE INDEX idx_document_version_time ON document_version (create_time DESC);

COMMENT ON TABLE document_version IS '文档版本历史表';

-- 6. 知识库质量检测表
DROP TABLE IF EXISTS kb_quality_check CASCADE;
CREATE TABLE kb_quality_check (
    id              BIGSERIAL                            PRIMARY KEY,
    kb_id           BIGINT                               NOT NULL,
    check_type      VARCHAR(50)                          NOT NULL,
    check_result    JSONB                                NOT NULL DEFAULT '{}',
    score           DECIMAL(5,2),
    suggestions     TEXT,
    check_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    is_delete       BOOLEAN   DEFAULT FALSE              NOT NULL
);

CREATE INDEX idx_kb_quality_check_kb ON kb_quality_check (kb_id);
CREATE INDEX idx_kb_quality_check_time ON kb_quality_check (check_time DESC);

COMMENT ON TABLE kb_quality_check IS '知识库质量检测表';
COMMENT ON COLUMN kb_quality_check.check_type IS '检测类型：COVERAGE-覆盖度, ACCURACY-准确度, COMPLETENESS-完整度';

-- 7. 初始化默认管理员账户
-- 用户名: admin, 密码: admin123 (MD5加密)
INSERT INTO sys_user (username, password, nickname, role, status)
VALUES ('admin', '0192023a7bbd73250516f069df18b500', '系统管理员', 'ADMIN', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;