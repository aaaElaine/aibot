<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider
      v-model:collapsed="collapsed"
      collapsible
      :trigger="null"
      theme="light"
      width="240"
    >
      <div class="logo">
        <span class="logo-icon">🤖</span>
        <h1 v-if="!collapsed">Chat Bot Admin</h1>
      </div>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        mode="inline"
        @click="handleMenuClick"
      >
        <a-menu-item key="dashboard">
          <template #icon>
            <DashboardOutlined />
          </template>
          <span>仪表盘</span>
        </a-menu-item>
        <a-menu-item key="kb">
          <template #icon>
            <BookOutlined />
          </template>
          <span>知识库管理</span>
        </a-menu-item>
        <a-menu-item key="document">
          <template #icon>
            <FileTextOutlined />
          </template>
          <span>文档管理</span>
        </a-menu-item>
        <a-menu-item key="quality">
          <template #icon>
            <CheckCircleOutlined />
          </template>
          <span>质量检测</span>
        </a-menu-item>

      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="header">
        <div class="header-left">
          <menu-fold-outlined
            v-if="!collapsed"
            class="trigger"
            @click="collapsed = !collapsed"
          />
          <menu-unfold-outlined
            v-else
            class="trigger"
            @click="collapsed = !collapsed"
          />
        </div>
        <div class="header-right">
          <a-dropdown>
            <a class="ant-dropdown-link" @click.prevent>
              <a-avatar style="background-color: #1890ff">
                <template #icon><UserOutlined /></template>
              </a-avatar>
              <span style="margin-left: 8px">{{ userStore.userInfo?.username || '管理员' }}</span>
            </a>
            <template #overlay>
              <a-menu>
                <a-menu-item key="logout" @click="handleLogout">
                  <LogoutOutlined />
                  <span style="margin-left: 8px">退出登录</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>
      <a-layout-content class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  DashboardOutlined,
  BookOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
  LogoutOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const collapsed = ref(false)
const selectedKeys = ref<string[]>(['dashboard'])
const openKeys = ref<string[]>([])

watch(
  () => route.path,
  (path) => {
    const segs = path.split('/').filter(Boolean)
    selectedKeys.value = [segs[0] || 'dashboard']
  },
  { immediate: true }
)

const handleMenuClick = ({ key }: { key: string }) => {
  router.push(`/${key}`)
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped lang="less">
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;

  .logo-icon {
    font-size: 32px;
  }

  h1 {
    margin: 0 0 0 12px;
    font-size: 18px;
    font-weight: 600;
    color: #1890ff;
    white-space: nowrap;
  }
}

.header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

  .header-left {
    .trigger {
      font-size: 18px;
      cursor: pointer;
      transition: color 0.3s;

      &:hover {
        color: #1890ff;
      }
    }
  }

  .header-right {
    .ant-dropdown-link {
      display: flex;
      align-items: center;
      color: rgba(0, 0, 0, 0.85);

      &:hover {
        color: #1890ff;
      }
    }
  }
}

.content {
  margin: 24px;
  padding: 24px;
  background: #fff;
  border-radius: 4px;
  min-height: 280px;
  overflow: auto;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>