<template>
  <div class="knowledge-base-list">
    <a-card :bordered="false">
      <div class="table-header">
        <a-space>
          <a-input
            v-model:value="searchParams.name"
            placeholder="搜索知识库名称"
            style="width: 250px"
            @pressEnter="handleSearch"
          >
            <template #prefix>
              <SearchOutlined />
            </template>
          </a-input>
          <a-select
            v-model:value="searchParams.status"
            placeholder="状态"
            style="width: 150px"
            allowClear
            @change="handleSearch"
          >
            <a-select-option value="ACTIVE">激活</a-select-option>
            <a-select-option value="INACTIVE">未激活</a-select-option>
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
        <a-button type="primary" @click="handleCreate">
          <template #icon><PlusOutlined /></template>
          创建知识库
        </a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="handleEdit(record)">{{ record.name }}</a>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 'ACTIVE' ? 'green' : 'orange'">
              {{ record.status === 'ACTIVE' ? '激活' : '未激活' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'documentCount'">
            <a-badge :count="record.documentCount || 0" :number-style="{ backgroundColor: '#52c41a' }" />
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ formatDate(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">
                编辑
              </a-button>
              <a-button type="link" size="small" @click="handleViewDocuments(record)">
                查看文档
              </a-button>
              <a-popconfirm
                title="确定要删除这个知识库吗？"
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
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined
} from '@ant-design/icons-vue'
import { kbApi, type KnowledgeBaseVO } from '@/api/kb'

const router = useRouter()

const loading = ref(false)
const tableData = ref<KnowledgeBaseVO[]>([])

const searchParams = reactive({
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
    title: '知识库名称',
    key: 'name',
    dataIndex: 'name'
  },
  {
    title: '描述',
    dataIndex: 'description',
    ellipsis: true
  },
  {
    title: '类型',
    dataIndex: 'type',
    width: 100
  },
  {
    title: '状态',
    key: 'status',
    dataIndex: 'status',
    width: 100
  },
  {
    title: '文档数',
    key: 'documentCount',
    dataIndex: 'documentCount',
    width: 100
  },
  {
    title: '创建时间',
    key: 'createTime',
    dataIndex: 'createTime',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    width: 250
  }
]

const fetchData = async () => {
  try {
    loading.value = true
    const res = await kbApi.getPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      name: searchParams.name || undefined,
      status: searchParams.status
    })
    tableData.value = res.records
    pagination.total = res.total
  } catch (error) {
    console.error('获取知识库列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
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

const handleCreate = () => {
  router.push('/kb/create')
}

const handleEdit = (record: KnowledgeBaseVO) => {
  router.push(`/kb/edit/${record.id}`)
}

const handleViewDocuments = (record: KnowledgeBaseVO) => {
  router.push({
    path: '/document',
    query: { kbId: record.id }
  })
}

const handleDelete = async (record: KnowledgeBaseVO) => {
  try {
    await kbApi.delete(record.id)
    message.success('删除成功')
    fetchData()
  } catch (error) {
    console.error('删除知识库失败:', error)
  }
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="less">
.knowledge-base-list {
  .table-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 16px;
  }
}
</style>