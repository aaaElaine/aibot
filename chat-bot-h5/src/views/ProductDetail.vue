<template>
  <div class="product-detail">
    <!-- 顶部导航栏 -->
    <van-nav-bar title="商品详情" fixed placeholder @click-left="goBack">
      <template #left>
        <van-icon name="arrow-left" size="20" />
      </template>
      <template #right>
        <van-icon name="share-o" size="20" @click="handleShare" />
      </template>
    </van-nav-bar>

    <!-- 商品图片 -->
    <div class="product-image-section">
      <img 
        :src="productImage" 
        :alt="productName"
        class="product-image"
        @error="handleImageError"
      />
      <div class="product-badge" v-if="category">{{ category }}</div>
    </div>

    <!-- 商品信息 -->
    <div class="product-info-section">
      <div class="price-row">
        <span class="price-symbol">¥</span>
        <span class="price-value">{{ price }}</span>
        <span class="price-original" v-if="originalPrice">¥{{ originalPrice }}</span>
      </div>
      <h1 class="product-title">{{ productName }}</h1>
      <p class="product-desc">{{ description }}</p>
    </div>

    <!-- 商品参数 -->
    <div class="product-specs">
      <div class="specs-title">商品参数</div>
      <div class="specs-list">
        <div class="spec-item">
          <span class="spec-label">商品编号</span>
          <span class="spec-value">{{ productId }}</span>
        </div>
        <div class="spec-item">
          <span class="spec-label">商品分类</span>
          <span class="spec-value">{{ category || '未分类' }}</span>
        </div>
        <div class="spec-item" v-if="salesCount">
          <span class="spec-label">销量</span>
          <span class="spec-value">{{ salesCount }} 件</span>
        </div>
        <div class="spec-item">
          <span class="spec-label">商品状态</span>
          <span class="spec-value status-active">在售</span>
        </div>
      </div>
    </div>

    <!-- 商品详情 -->
    <div class="product-detail-section">
      <div class="detail-title">商品详情</div>
      <div class="detail-content">
        <p>本商品由苏福万家平台精选，质量保证，售后无忧。</p>
        <p>苏福万家致力于为您提供优质的适老化改造产品和服务，让您的生活更加便捷、安全、舒适。</p>
        <p>如有任何疑问，请随时联系我们的客服小助手，我们将竭诚为您服务~</p>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="bottom-bar">
      <div class="bar-item" @click="handleFavorite">
        <van-icon :name="isFavorited ? 'star' : 'star-o'" size="22" :color="isFavorited ? '#ff4d4f' : '#666'" />
        <span>收藏</span>
      </div>
      <div class="bar-item" @click="handleCart">
        <van-icon name="cart-o" size="22" color="#666" />
        <span>购物车</span>
      </div>
      <van-button type="primary" block round class="buy-btn" @click="handleBuy">
        立即购买
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'

const route = useRoute()
const router = useRouter()

// 从路由参数获取商品信息
const productId = computed(() => route.query.id as string || '0')
const productName = computed(() => route.query.name as string || '未知商品')
const price = computed(() => route.query.price as string || '0.00')
const description = computed(() => route.query.description as string || '暂无商品描述')
const category = computed(() => route.query.category as string || '')
const originalPrice = computed(() => {
  // 模拟原价
  const p = parseFloat(price.value)
  if (p > 0) {
    return (p * 1.2).toFixed(2)
  }
  return ''
})
const salesCount = computed(() => Math.floor(Math.random() * 500) + 50)

// 商品图片 - 使用占位图
const productImage = computed(() => {
  const imgUrl = route.query.imageUrl as string
  if (imgUrl && imgUrl.startsWith('http')) {
    return imgUrl
  }
  // 使用文本描述生成的占位图
  return `https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=${encodeURIComponent(`product photo of ${productName.value}, professional product photography on white background, e-commerce product image, high quality, 4k`)}&image_size=square_hd`
})

// 收藏状态
const isFavorited = ref(false)

function goBack() {
  router.back()
}

function handleShare() {
  showToast('分享功能开发中~')
}

function handleFavorite() {
  isFavorited.value = !isFavorited.value
  if (isFavorited.value) {
    showSuccessToast('已加入收藏')
  } else {
    showToast('已取消收藏')
  }
}

function handleCart() {
  showToast('购物车功能开发中~')
}

function handleBuy() {
  showSuccessToast('已加入购买清单，客服将尽快联系您~')
}

function handleImageError(e: Event) {
  const img = e.target as HTMLImageElement
  // 图片加载失败时使用文字占位
  img.style.display = 'none'
}
</script>

<style lang="scss" scoped>
.product-detail {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 70px;
}

.product-image-section {
  position: relative;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: center;

  .product-image {
    width: 100%;
    max-width: 375px;
    height: 375px;
    object-fit: cover;
    border-radius: 12px;
    background: #eee;
  }

  .product-badge {
    position: absolute;
    top: 16px;
    left: 16px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    padding: 4px 12px;
    border-radius: 12px;
    font-size: 12px;
  }
}

.product-info-section {
  background: #fff;
  padding: 16px;
  margin-bottom: 10px;

  .price-row {
    display: flex;
    align-items: baseline;
    margin-bottom: 12px;

    .price-symbol {
      font-size: 18px;
      font-weight: bold;
      color: #ff4d4f;
    }

    .price-value {
      font-size: 28px;
      font-weight: bold;
      color: #ff4d4f;
      margin-right: 8px;
    }

    .price-original {
      font-size: 14px;
      color: #ccc;
      text-decoration: line-through;
    }
  }

  .product-title {
    font-size: 18px;
    font-weight: 600;
    color: #333;
    margin-bottom: 8px;
    line-height: 1.5;
  }

  .product-desc {
    font-size: 14px;
    color: #666;
    line-height: 1.6;
  }
}

.product-specs {
  background: #fff;
  padding: 16px;
  margin-bottom: 10px;

  .specs-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid #f0f0f0;
  }

  .specs-list {
    .spec-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 0;
      font-size: 14px;

      .spec-label {
        color: #999;
      }

      .spec-value {
        color: #333;

        &.status-active {
          color: #52c41a;
        }
      }
    }
  }
}

.product-detail-section {
  background: #fff;
  padding: 16px;
  margin-bottom: 10px;

  .detail-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid #f0f0f0;
  }

  .detail-content {
    font-size: 14px;
    color: #666;
    line-height: 1.8;

    p {
      margin-bottom: 8px;
    }
  }
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 16px;
  padding-bottom: env(safe-area-inset-bottom);
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.08);

  .bar-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    padding: 0 12px;
    cursor: pointer;

    span {
      font-size: 11px;
      color: #666;
    }
  }

  .buy-btn {
    flex: 1;
    margin-left: 12px;
    background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
    border: none;
  }
}
</style>
