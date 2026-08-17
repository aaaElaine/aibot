CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

DROP TABLE IF EXISTS chat_message CASCADE;

CREATE TABLE chat_message (
    id              BIGSERIAL                            PRIMARY KEY,
    conversation_id VARCHAR(64)                          NOT NULL,
    message_type    VARCHAR(20)                          NOT NULL,
    content         TEXT                                 NOT NULL,
    metadata        JSONB                                NOT NULL DEFAULT '{}',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    is_delete       BOOLEAN   DEFAULT FALSE              NOT NULL
);

CREATE INDEX idx_chat_message_conversation_id ON chat_message (conversation_id);
CREATE INDEX idx_chat_message_create_time ON chat_message (create_time DESC);
CREATE INDEX idx_chat_message_type ON chat_message (message_type);
CREATE INDEX idx_chat_message_not_deleted ON chat_message (conversation_id, create_time) WHERE is_delete = FALSE;

ALTER TABLE chat_message ADD CONSTRAINT chk_message_type
    CHECK (message_type IN ('USER', 'ASSISTANT', 'SYSTEM'));

DROP TABLE IF EXISTS vector_store CASCADE;

CREATE TABLE vector_store (
    id        UUID DEFAULT uuid_generate_v4()          PRIMARY KEY,
    content   TEXT                                     NOT NULL,
    metadata  JSONB                                    NOT NULL DEFAULT '{}',
    embedding VECTOR(1024)                             NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP    NOT NULL,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP    NOT NULL
);

CREATE INDEX idx_vector_store_embedding ON vector_store
    USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
CREATE INDEX idx_vector_store_create_time ON vector_store (create_time DESC);
CREATE INDEX idx_vector_store_metadata ON vector_store USING gin (metadata);