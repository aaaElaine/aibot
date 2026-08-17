<template>
  <div class="document-list">
    <a-card :bordered="false">
      <div class="table-header">
        <a-space>
          <a-select
            v-model:value="searchParams.kbId"
            placeholder="选择知识库"
            style="width: 200px"
            allowClear
            @change="handleSearch"
          >
            <a-select-option v-for="kb in kbList" :key="kb.id" :value="kb.id">
              {{ kb.name }}
            </a-select-option>
          </a-select>
          <a-input
            v-model:value="searchParams.name"
            placeholder="搜索文档名称"
            style="width: 250px"
            allowClear
            @pressEnter="handleSearch"
          >
            <template #prefix>
              <SearchOutlined />
            </template>
          </a-input>
          <a-select
            v-model:value="searchParams.status"
            placeholder="状态"
            style="width: 120px"
            allowClear
            @change="handleSearch"
          >
            <a-select-option value="PENDING">待处理</a-select-option>
            <a-select-option value="PROCESSING">处理中</a-select-option>
            <a-select-option value="COMPLETED">已完成</a-select-option>
            <a-select-option value="FAILED">失败</a-select-option>
          </a-select>
          <a-button type="primary" @click="handleSearch">
            <template #icon><SearchOutlined /></template>
            搜索
          </a-button>
          <a-button @click="handleReset">
            <template #icon><ReloadOutlined /></template>
            重置
          </a-button>
        </a-space>
        <a-button type="primary" @click="handleUpload">
          <template #icon><UploadOutlined /></template>
          上传文档
        </a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        rowKey="id"
        :scroll="{ x: 'max-content' }"
        size="middle"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <a-space>
              <FileTextOutlined />
              <span>{{ record.title }}</span>
            </a-space>
          </template>
          <template v-else-if="column.key === 'fileType'">
            <a-tag v-if="record.fileType" :color="getTypeColor(record.fileType)">
              {{ record.fileType.toUpperCase() }}
            </a-tag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'fileSize'">
            {{ formatFileSize(record.fileSize) }}
          </template>
          <template v-else-if="column.key === 'categoryName'">
            {{ record.categoryName || '-' }}
          </template>
          <template v-else-if="column.key === 'vectorStatus'">
            <a-tag :color="getStatusColor(record.vectorStatus)">
              {{ getStatusText(record.vectorStatus) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ formatDate(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleDownload(record)">
                下载
              </a-button>
              <a-popconfirm
                title="确定要删除这个文档吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(record)"
              >
                <a-button type="link" size="small" danger>
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  UploadOutlined,
  FileTextOutlined
} from '@ant-design/icons-vue'
import { kbApi, type KnowledgeBaseVO } from '@/api/kb'
import { documentApi, type DocumentVO } from '@/api/document'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const tableData = ref<DocumentVO[]>([])
const kbList = ref<KnowledgeBaseVO[]>([])

const searchParams = reactive({
  kbId: undefined as number | undefined,
  name: '',
  status: undefined as string | undefined
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  {
    title: '文档名称',
    key: 'title',
    dataIndex: 'title',
    width: 200,
    ellipsis: true
  },
  {
    title: '类型',
    key: 'fileType',
    dataIndex: 'fileType',
    width: 80
  },
  {
    title: '大小',
    key: 'fileSize',
    dataIndex: 'fileSize',
    width: 100
  },
  {
    title: '分类',
    key: 'categoryName',
    dataIndex: 'categoryName',
    width: 100
  },
  {
    title: '状态',
    key: 'vectorStatus',
    dataIndex: 'vectorStatus',
    width: 90
  },
  {
    title: '版本',
    dataIndex: 'version',
    width: 60
  },
  {
    title: '创建时间',
    key: 'createTime',
    dataIndex: 'createTime',
    width: 160
  },
  {
    title: '操作',
    key: 'action',
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

const fetchData = async () => {
  try {
    loading.value = true
    const res = await documentApi.getPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      kbId: searchParams.kbId,
      name: searchParams.name || undefined,
      status: searchParams.status
    })
    tableData.value = res.records
    pagination.total = res.total
  } catch (error) {
    console.error('获取文档列表失败:', error)
    message.error('获取文档列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  searchParams.kbId = undefined
  searchParams.name = ''
  searchParams.status = undefined
  pagination.current = 1
  fetchData()
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const handleUpload = () => {
  router.push('/document/upload')
}

const handleDownload = async (record: DocumentVO) => {
  try {
    if (!record.filePath) {
      message.error('文件路径不存在')
      return
    }
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
    const url = `${baseUrl}${documentApi.downloadUrl(record.id)}`
    const response = await fetch(url, {
      headers: {
        'Authorization': `Bearer ${useUserStore().token}`
      }
    })
    if (!response.ok) {
      throw new Error('下载失败')
    }
    const blob = await response.blob()
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = record.title || `document_${record.id}`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)
  } catch (error) {
    console.error('下载失败:', error)
    message.error('下载失败')
  }
}

const handleDelete = async (record: DocumentVO) => {
  try {
    await documentApi.delete(record.id)
    message.success('删除成功')
    fetchData()
  } catch (error) {
    console.error('删除文档失败:', error)
  }
}

const formatFileSize = (size?: number) => {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  return (size / (1024 * 1024)).toFixed(2) + ' MB'
}

const getTypeColor = (type: string) => {
  const colors: Record<string, string> = {
    pdf: 'red',
    doc: 'blue',
    docx: 'blue',
    txt: 'default',
    md: 'green',
    markdown: 'green',
    xls: 'green',
    xlsx: 'green'
  }
  return colors[type.toLowerCase()] || 'default'
}

const getStatusColor = (status?: string) => {
  switch (status) {
    case 'COMPLETED':
      return 'green'
    case 'PROCESSING':
      return 'blue'
    case 'FAILED':
      return 'red'
    case 'PENDING':
      return 'orange'
    default:
      return 'default'
  }
}

const getStatusText = (status?: string) => {
  switch (status) {
    case 'COMPLETED':
      return '已完成'
    case 'PROCESSING':
      return '处理中'
    case 'FAILED':
      return '失败'
    case 'PENDING':
      return '待处理'
    default:
      return status || '-'
  }
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  try {
    const date = new Date(dateStr)
    if (isNaN(date.getTime())) return dateStr
    const pad = (n: number) => n.toString().padStart(2, '0')
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  } catch {
    return dateStr
  }
}

onMounted(() => {
  const kbId = route.query.kbId
  if (kbId) {
    searchParams.kbId = Number(kbId)
  }
  fetchKbList()
  fetchData()
})
</script>

<style scoped lang="less">
.document-list {
  .table-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 16px;
    flex-wrap: wrap;
    gap: 12px;
  }
}
</style>
