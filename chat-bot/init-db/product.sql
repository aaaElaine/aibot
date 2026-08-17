-- ================================================================
-- 商品表 - 存储苏福万家商城商品
-- ================================================================
DROP TABLE IF EXISTS product CASCADE;

CREATE TABLE product (
    id              BIGSERIAL                            PRIMARY KEY,
    name            VARCHAR(200)                         NOT NULL,
    description     TEXT                                 NOT NULL,
    price           DECIMAL(10, 2)                       NOT NULL,
    original_price  DECIMAL(10, 2),
    image_url       VARCHAR(500),
    category        VARCHAR(100)                         NOT NULL,
    brand           VARCHAR(100),
    product_url     VARCHAR(500),
    sales_count     INTEGER   DEFAULT 0,
    status          VARCHAR(20)  DEFAULT 'ONLINE',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    is_delete       BOOLEAN   DEFAULT FALSE              NOT NULL
);

COMMENT ON TABLE product IS '商品表 - 存储苏福万家商城商品';
COMMENT ON COLUMN product.id IS '主键ID';
COMMENT ON COLUMN product.name IS '商品名称';
COMMENT ON COLUMN product.description IS '商品描述';
COMMENT ON COLUMN product.price IS '商品价格';
COMMENT ON COLUMN product.original_price IS '原价';
COMMENT ON COLUMN product.image_url IS '商品图片URL';
COMMENT ON COLUMN product.category IS '商品分类';
COMMENT ON COLUMN product.brand IS '品牌';
COMMENT ON COLUMN product.product_url IS '商品详情页URL';
COMMENT ON COLUMN product.sales_count IS '销量';
COMMENT ON COLUMN product.status IS '状态 ONLINE/OFFLINE';
COMMENT ON COLUMN product.create_time IS '创建时间';
COMMENT ON COLUMN product.update_time IS '更新时间';
COMMENT ON COLUMN product.is_delete IS '删除标志';

CREATE INDEX idx_product_category ON product (category);
CREATE INDEX idx_product_status ON product (status);
CREATE INDEX idx_product_name ON product USING gin (to_tsvector('simple', name));

-- 插入示例商品数据
INSERT INTO product (name, description, price, original_price, image_url, category, brand, product_url, sales_count, status) VALUES
('适老化扶手', '卫生间安全扶手，防滑防摔，适老化改造必备', 128.00, 198.00, 'https://example.com/images/handrail.jpg', '适老化改造', '苏福', '/pages/product/detail?id=1', 520, 'ONLINE'),
('护理床', '家用多功能护理床，电动升降，医院级品质', 3580.00, 4800.00, 'https://example.com/images/bed.jpg', '康复护理', '苏福', '/pages/product/detail?id=2', 128, 'ONLINE'),
('助行器', '轻便折叠助行器，老年人康复辅助器具', 268.00, 368.00, 'https://example.com/images/walker.jpg', '康复护理', '苏福', '/pages/product/detail?id=3', 356, 'ONLINE'),
('智能手环', '健康监测智能手环，心率血氧监测，跌倒报警', 399.00, 599.00, 'https://example.com/images/bracelet.jpg', '智能穿戴', '苏福', '/pages/product/detail?id=4', 892, 'ONLINE'),
('营养膳食套餐', '营养师定制养老膳食套餐，低盐低脂，营养均衡', 199.00, 299.00, 'https://example.com/images/meal.jpg', '健康餐饮', '苏福', '/pages/product/detail?id=5', 1250, 'ONLINE'),
('上门按摩服务', '专业按摩师上门服务，缓解肩颈腰背疲劳', 198.00, 298.00, 'https://example.com/images/massage.jpg', '健康服务', '苏福', '/pages/product/detail?id=6', 234, 'ONLINE'),
('康复理疗仪', '家用康复理疗仪，缓解肌肉酸痛，促进血液循环', 598.00, 898.00, 'https://example.com/images/therapy.jpg', '康复护理', '苏福', '/pages/product/detail?id=7', 445, 'ONLINE'),
('适老化马桶', '智能适老化马桶，一键冲水，安全舒适', 2880.00, 3580.00, 'https://example.com/images/toilet.jpg', '适老化改造', '苏福', '/pages/product/detail?id=8', 89, 'ONLINE'),
('老年大学课程', '线上老年大学课程，书法、绘画、太极等', 99.00, 199.00, 'https://example.com/images/class.jpg', '教育培训', '苏福', '/pages/product/detail?id=9', 2100, 'ONLINE'),
('银发旅游', '专为老年人设计的康养旅游，全程医护陪护', 2980.00, 3980.00, 'https://example.com/images/travel.jpg', '旅游出行', '苏福乐游', '/pages/product/detail?id=10', 156, 'ONLINE'),
('居家养老服务', '专业护理员上门，提供生活照料、康复护理', 199.00, 299.00, 'https://example.com/images/care.jpg', '养老服务', '苏福惠', '/pages/product/detail?id=11', 567, 'ONLINE'),
('适老化改造套餐', '全屋适老化改造套餐，地面、扶手、灯光等', 9800.00, 12800.00, 'https://example.com/images/renovation.jpg', '适老化改造', '苏福', '/pages/product/detail?id=12', 45, 'ONLINE'),
('助听器', '数字助听器，高清音质，适合老年人使用', 1980.00, 2980.00, 'https://example.com/images/hearing.jpg', '康复护理', '苏福', '/pages/product/detail?id=13', 234, 'ONLINE'),
('血糖仪', '家用血糖仪，智能传输数据，异常预警', 268.00, 398.00, 'https://example.com/images/glucose.jpg', '健康监测', '苏福', '/pages/product/detail?id=14', 678, 'ONLINE'),
('电动轮椅', '智能电动轮椅，轻便折叠，长续航', 3980.00, 5280.00, 'https://example.com/images/wheelchair.jpg', '康复护理', '苏福', '/pages/product/detail?id=15', 178, 'ONLINE');
