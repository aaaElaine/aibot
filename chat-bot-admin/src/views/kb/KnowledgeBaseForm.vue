<template>
  <div class="knowledge-base-form">
    <a-card :bordered="false">
      <a-page-header
        :title="isEdit ? '编辑知识库' : '创建知识库'"
        @back="handleBack"
      >
        <template #extra>
          <a-button @click="handleBack">取消</a-button>
          <a-button type="primary" :loading="loading" @click="handleSubmit">
            保存
          </a-button>
        </template>
      </a-page-header>

      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 14 }"
        style="margin-top: 24px"
      >
        <a-form-item label="知识库名称" name="name">
          <a-input
            v-model:value="formState.name"
            placeholder="请输入知识库名称"
            maxlength="100"
          />
        </a-form-item>

        <a-form-item label="描述" name="description">
          <a-textarea
            v-model:value="formState.description"
            placeholder="请输入知识库描述"
            :rows="4"
            maxlength="500"
            show-count
          />
        </a-form-item>

        <a-form-item label="类型" name="type">
          <a-select
            v-model:value="formState.type"
            placeholder="请选择知识库类型"
          >
            <a-select-option value="DOCUMENT">文档型</a-select-option>
            <a-select-option value="QA">问答型</a-select-option>
            <a-select-option value="MIXED">混合型</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item v-if="isEdit" label="状态" name="status">
          <a-radio-group v-model:value="formState.status">
            <a-radio value="ACTIVE">激活</a-radio>
            <a-radio value="INACTIVE">未激活</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import { kbApi, type KnowledgeBaseUpdateRequest } from '@/api/kb'

const router = useRouter()
const route = useRoute()

const formRef = ref<FormInstance>()
const loading = ref(false)

const routeId = computed(() => Number(route.params.id))
const isEdit = computed(() => !!routeId.value)

const formState = reactive<KnowledgeBaseUpdateRequest>({
  id: 0,
  name: '',
  description: '',
  type: 'DOCUMENT',
  status: 'ACTIVE'
})

const rules: Record<string, Rule[]> = {
  name: [
    { required: true, message: '请输入知识库名称', trigger: 'blur' },
    { min: 2, max: 100, message: '名称长度在2-100个字符之间', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择知识库类型', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const fetchDetail = async () => {
  if (!routeId.value) return
  
  try {
    loading.value = true
    const res = await kbApi.getById(routeId.value)
    Object.assign(formState, res)
  } catch (error) {
    console.error('获取知识库详情失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    loading.value = true

    if (isEdit.value) {
      formState.id = routeId.value
      await kbApi.update(formState)
      message.success('更新成功')
    } else {
      await kbApi.create({
        name: formState.name,
        description: formState.description,
        type: formState.type
      })
      message.success('创建成功')
    }

    router.push('/kb')
  } catch (error) {
    console.error('保存知识库失败:', error)
  } finally {
    loading.value = false
  }
}

const handleBack = () => {
  router.back()
}

onMounted(() => {
  if (isEdit.value) {
    fetchDetail()
  }
})
</script>

<style scoped lang="less">
.knowledge-base-form {
  :deep(.ant-page-header) {
    padding: 0;
  }
}
</style>