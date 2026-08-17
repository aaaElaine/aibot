<template>
  <div class="quality-check">
    <a-card :bordered="false">
      <div class="check-header">
        <a-space>
          <a-select
            v-model:value="selectedKbId"
            placeholder="选择知识库"
            style="width: 300px"
          >
            <a-select-option v-for="kb in kbList" :key="kb.id" :value="kb.id">
              {{ kb.name }}
            </a-select-option>
          </a-select>
          <a-button
            type="primary"
            :loading="checking"
            :disabled="!selectedKbId"
            @click="handleCheck"
          >
            <template #icon><PlayCircleOutlined /></template>
            执行检测
          </a-button>
        </a-space>
      </div>

      <a-divider />

      <div v-if="checkResult" class="check-result">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-card class="stat-card">
              <a-statistic
                title="总分"
                :value="checkResult.score || 0"
                suffix="分"
                :value-style="{ color: getScoreColor(checkResult.score) }"
              />
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="stat-card">
              <a-statistic
                title="总文档数"
                :value="checkResult.totalDocuments"
              />
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="stat-card">
              <a-statistic
                title="通过文档"
                :value="checkResult.passedDocuments"
                :value-style="{ color: '#3f8600' }"
              />
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="stat-card">
              <a-statistic
                title="失败文档"
                :value="checkResult.failedDocuments"
                :value-style="{ color: '#cf1322' }"
              />
            </a-card>
          </a-col>
        </a-row>

        <a-card v-if="checkResult.suggestions" title="检测建议" style="margin-top: 16px">
          <a-alert
            v-for="(tip, idx) in suggestionList"
            :key="idx"
            :message="tip"
            type="info"
            show-icon
            style="margin-bottom: 8px"
          />
          <a-empty v-if="suggestionList.length === 0" description="暂无建议，知识库状态良好" />
        </a-card>

        <a-card v-if="checkResult.issues && checkResult.issues.length > 0" title="检测问题" style="margin-top: 16px">
          <a-table
            :columns="issueColumns"
            :data-source="checkResult.issues"
            :pagination="false"
            rowKey="documentId"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'severity'">
                <a-tag :color="getSeverityColor(record.severity)">
                  {{ getSeverityText(record.severity) }}
                </a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </div>

      <a-empty
        v-else
        description="请选择知识库并执行检测"
        style="margin-top: 48px"
      />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlayCircleOutlined } from '@ant-design/icons-vue'
import { kbApi, type KnowledgeBaseVO } from '@/api/kb'
import { qualityApi, type QualityCheckResult } from '@/api/quality'

const kbList = ref<KnowledgeBaseVO[]>([])
const selectedKbId = ref<number>()
const checking = ref(false)
const checkResult = ref<QualityCheckResult | null>(null)

const suggestionList = computed(() => {
  if (!checkResult.value?.suggestions) return []
  return checkResult.value.suggestions
    .split('\n')
    .map(s => s.trim())
    .filter(s => s.length > 0)
})

const issueColumns = [
  {
    title: '文档名称',
    dataIndex: 'documentName',
    key: 'documentName'
  },
  {
    title: '问题类型',
    dataIndex: 'type',
    key: 'type'
  },
  {
    title: '问题描述',
    dataIndex: 'description',
    key: 'description'
  },
  {
    title: '严重程度',
    key: 'severity',
    dataIndex: 'severity',
    width: 120
  }
]

const fetchKbList = async () => {
  try {
    const res = await kbApi.getPage({ pageNum: 1, pageSize: 100 })
    kbList.value = res.records
  } catch (error) {
    console.error('获取知识库列表失败:', error)
  }
}

const handleCheck = async () => {
  if (!selectedKbId.value) {
    message.warning('请选择知识库')
    return
  }

  try {
    checking.value = true
    message.loading('正在执行质量检测...', 0)
    
    const res = await qualityApi.check(selectedKbId.value)
    checkResult.value = res
    
    message.destroy()
    message.success('检测完成')
  } catch (error) {
    message.destroy()
    message.error('检测失败')
    console.error('执行质量检测失败:', error)
  } finally {
    checking.value = false
  }
}

const getScoreColor = (score?: number) => {
  if (!score) return '#999'
  if (score >= 90) return '#3f8600'
  if (score >= 70) return '#faad14'
  return '#cf1322'
}

const getSeverityColor = (severity: string) => {
  switch (severity) {
    case 'high':
      return 'red'
    case 'medium':
      return 'orange'
    case 'low':
      return 'blue'
    default:
      return 'default'
  }
}

const getSeverityText = (severity: string) => {
  switch (severity) {
    case 'high':
      return '高'
    case 'medium':
      return '中'
    case 'low':
      return '低'
    default:
      return '未知'
  }
}

onMounted(() => {
  fetchKbList()
})
</script>

<style scoped lang="less">
.quality-check {
  .check-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .check-result {
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
}
</style>