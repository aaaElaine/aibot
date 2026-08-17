<template>
  <div class="document-upload">
    <a-card :bordered="false">
      <a-page-header
        title="上传文档"
        @back="handleBack"
      >
        <template #extra>
          <a-button @click="handleBack">返回</a-button>
        </template>
      </a-page-header>

      <a-alert
        type="info"
        show-icon
        message="上传说明"
        description="文件上传后将立即保存，向量化在后台异步处理。大文件上传后可以离开当前页面，稍后可在文档列表查看处理进度。"
        style="margin-top: 16px; margin-bottom: 16px"
      />

      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 14 }"
        style="margin-top: 8px"
      >
        <a-form-item label="目标知识库" name="kbId">
          <a-select
            v-model:value="formState.kbId"
            placeholder="请选择目标知识库"
            style="width: 100%"
          >
            <a-select-option v-for="kb in kbList" :key="kb.id" :value="kb.id">
              {{ kb.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="文档文件" name="file">
          <a-upload-dragger
            v-model:fileList="formState.fileList"
            :multiple="true"
            :before-upload="beforeUpload"
            @remove="handleRemove"
          >
            <p class="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
            <p class="ant-upload-hint">
              支持单个或批量上传，支持的文件类型：PDF、Word、TXT、Markdown等
            </p>
          </a-upload-dragger>
        </a-form-item>

        <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
          <a-button
            type="primary"
            :loading="uploading"
            :disabled="formState.fileList.length === 0"
            @click="handleUpload"
          >
            <template #icon><UploadOutlined /></template>
            {{ uploading ? '上传中...' : '开始上传' }}
          </a-button>
        </a-form-item>
      </a-form>

      <a-divider v-if="uploadResults.length > 0" />

      <div v-if="uploadResults.length > 0">
        <h3 style="margin-bottom: 16px">上传结果</h3>
        <a-list
          :data-source="uploadResults"
          item-layout="horizontal"
        >
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta :description="item.message">
                <template #title>
                  {{ item.fileName }}
                </template>
                <template #avatar>
                  <a-avatar
                    :style="{
                      backgroundColor: item.success ? '#52c41a' : '#ff4d4f'
                    }"
                  >
                    <template #icon>
                      <CheckOutlined v-if="item.success" />
                      <CloseOutlined v-else />
                    </template>
                  </a-avatar>
                </template>
              </a-list-item-meta>
            </a-list-item>
          </template>
        </a-list>

        <div style="margin-top: 16px; text-align: right">
          <a-button type="link" @click="handleBack">返回文档列表</a-button>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { UploadProps } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import {
  InboxOutlined,
  UploadOutlined,
  CheckOutlined,
  CloseOutlined
} from '@ant-design/icons-vue'
import { kbApi, type KnowledgeBaseVO } from '@/api/kb'
import { documentApi } from '@/api/document'

const router = useRouter()

const formRef = ref()
const uploading = ref(false)
const kbList = ref<KnowledgeBaseVO[]>([])

interface UploadResult {
  fileName: string
  success: boolean
  message: string
}

const uploadResults = ref<UploadResult[]>([])

const formState = reactive({
  kbId: undefined as number | undefined,
  fileList: [] as any[]
})

const rules: Record<string, Rule[]> = {
  kbId: [
    { required: true, message: '请选择目标知识库', trigger: 'change' }
  ]
}

const fetchKbList = async () => {
  try {
    const res = await kbApi.getPage({ pageNum: 1, pageSize: 100 })
    kbList.value = res.records
  } catch (error) {
    console.error('获取知识库列表失败:', error)
  }
}

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  // 验证文件类型
  const allowedTypes = ['pdf', 'doc', 'docx', 'txt', 'md', 'markdown', 'xls', 'xlsx']
  const extension = file.name.split('.').pop()?.toLowerCase() || ''
  if (!allowedTypes.includes(extension)) {
    message.error(`不支持的文件类型: .${extension}`)
    return false
  }
  // 验证文件大小（100MB限制）
  if (file.size > 100 * 1024 * 1024) {
    message.error('文件大小不能超过 100MB')
    return false
  }
  formState.fileList = [...formState.fileList, file]
  return false
}

const handleRemove = (file: any) => {
  const index = formState.fileList.indexOf(file)
  const newFileList = formState.fileList.slice()
  newFileList.splice(index, 1)
  formState.fileList = newFileList
}

const handleUpload = async () => {
  try {
    await formRef.value?.validate()

    if (!formState.kbId) {
      message.warning('请选择目标知识库')
      return
    }

    if (formState.fileList.length === 0) {
      message.warning('请选择要上传的文件')
      return
    }

    uploading.value = true
    uploadResults.value = []

    // 并行上传所有文件
    const uploadPromises = formState.fileList.map(async (fileItem) => {
      try {
        const file = fileItem.originFileObj as File
        const res = await documentApi.upload(file, formState.kbId, undefined)
        return {
          fileName: file.name,
          success: true,
          message: '上传成功，后台正在处理向量化'
        }
      } catch (error: any) {
        const errorMessage = error?.userMessage || error?.response?.data?.message || error?.message || '上传失败'
        console.error('上传失败:', fileItem.name, error)
        return {
          fileName: fileItem.name,
          success: false,
          message: errorMessage
        }
      }
    })

    uploadResults.value = await Promise.all(uploadPromises)

    const successCount = uploadResults.value.filter(r => r.success).length
    if (successCount === uploadResults.value.length) {
      message.success(`全部 ${successCount} 个文件上传成功，向量化正在后台处理`)
    } else {
      message.warning(`成功上传 ${successCount}/${uploadResults.value.length} 个文件`)
    }

    formState.fileList = []
  } catch (error) {
    console.error('上传文档失败:', error)
  } finally {
    uploading.value = false
  }
}

const handleBack = () => {
  router.back()
}

onMounted(() => {
  fetchKbList()
})
</script>

<style scoped lang="less">
.document-upload {
  :deep(.ant-page-header) {
    padding: 0;
  }

  :deep(.ant-upload-drag) {
    background: #fafafa;
    border: 1px dashed #d9d9d9;
    border-radius: 4px;

    &:hover {
      border-color: #1890ff;
    }
  }
}
</style>