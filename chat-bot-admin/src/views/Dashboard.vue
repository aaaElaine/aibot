<template>
  <div class="dashboard">
    <a-row :gutter="16">
      <a-col :span="6">
        <a-card class="stat-card">
          <a-statistic
            title="知识库总数"
            :value="stats.kbCount"
            :value-style="{ color: '#3f8600' }"
          >
            <template #prefix>
              <BookOutlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card class="stat-card">
          <a-statistic
            title="文档总数"
            :value="stats.documentCount"
            :value-style="{ color: '#1890ff' }"
          >
            <template #prefix>
              <FileTextOutlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card class="stat-card">
          <a-statistic
            title="检测通过率"
            :value="stats.passRate"
            suffix="%"
            :value-style="{ color: '#cf1322' }"
          >
            <template #prefix>
              <CheckCircleOutlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card class="stat-card">
          <a-statistic
            title="待处理问题"
            :value="stats.issueCount"
            :value-style="{ color: '#faad14' }"
          >
            <template #prefix>
              <WarningOutlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="12">
        <a-card title="最近知识库" :bordered="false">
          <a-list
            :data-source="recentKbs"
            :loading="loading"
          >
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta :description="item.description">
                  <template #title>
                    <a @click="goToKb(item.id)">{{ item.name }}</a>
                  </template>
                </a-list-item-meta>
                <template #actions>
                  <span>{{ item.documentCount }} 个文档</span>
                </template>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="最近质量检测" :bordered="false">
          <a-list
            :data-source="recentChecks"
            :loading="loading"
          >
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta :description="item.checkTime">
                  <template #title>
                    {{ item.kbName }}
                  </template>
                </a-list-item-meta>
                <template #actions>
                  <a-tag :color="item.status === 'PASSED' ? 'green' : 'orange'">
                    {{ item.status === 'PASSED' ? '通过' : '未通过' }}
                  </a-tag>
                </template>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="24">
        <a-card title="快捷操作" :bordered="false">
          <a-row :gutter="16">
            <a-col :span="6">
              <a-button type="primary" block @click="goToCreateKb">
                <template #icon><PlusOutlined /></template>
                创建知识库
              </a-button>
            </a-col>
            <a-col :span="6">
              <a-button block @click="goToUploadDocument">
                <template #icon><UploadOutlined /></template>
                上传文档
              </a-button>
            </a-col>
            <a-col :span="6">
              <a-button block @click="goToQualityCheck">
                <template #icon><SafetyCertificateOutlined /></template>
                质量检测
              </a-button>
            </a-col>
            <a-col :span="6">
              <a-button block @click="goToKbList">
                <template #icon><UnorderedListOutlined /></template>
                知识库列表
              </a-button>
            </a-col>
          </a-row>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  BookOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  PlusOutlined,
  UploadOutlined,
  SafetyCertificateOutlined,
  UnorderedListOutlined
} from '@ant-design/icons-vue'
import { kbApi, type KnowledgeBaseVO } from '@/api/kb'
import { qualityApi, type QualityCheckResult } from '@/api/quality'

const router = useRouter()

const loading = ref(false)
const stats = ref({
  kbCount: 0,
  documentCount: 0,
  passRate: 0,
  issueCount: 0
})
const recentKbs = ref<KnowledgeBaseVO[]>([])
const recentChecks = ref<QualityCheckResult[]>([])

const fetchData = async () => {
  try {
    loading.value = true
    const kbRes = await kbApi.getPage({ pageNum: 1, pageSize: 5 })
    recentKbs.value = kbRes.records
    stats.value.kbCount = kbRes.total
  } catch (error) {
    console.error('获取数据失败:', error)
  } finally {
    loading.value = false
  }
}

const goToKb = (id: number) => {
  router.push(`/kb/edit/${id}`)
}

const goToCreateKb = () => {
  router.push('/kb/create')
}

const goToUploadDocument = () => {
  router.push('/document/upload')
}

const goToQualityCheck = () => {
  router.push('/quality')
}

const goToKbList = () => {
  router.push('/kb')
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="less">
.dashboard {
  .stat-card {
    text-align: center;
    
    :deep(.ant-statistic-title) {
      font-size: 14px;
      margin-bottom: 8px;
    }
    
    :deep(.ant-statistic-content) {
      font-size: 24px;
    }
  }
}
</style>