<template>
  <div>
    <div class="page-head">
      <div>
        <h1 class="page-title">副本管理</h1>
        <p class="page-sub">副本内容全部保存在 MySQL，发布后进入玩家大厅与匹配池</p>
      </div>
      <div style="display: flex; gap: 10px">
        <el-button :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增副本</el-button>
      </div>
    </div>

    <div class="panel">
      <el-table v-loading="loading" :data="scripts">
        <el-table-column prop="title" label="副本名称" min-width="180" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="dark">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requiredPlayers" label="开局人数" width="100" />
        <el-table-column prop="characterCount" label="角色数" width="90" />
        <el-table-column prop="clueCount" label="线索数" width="90" />
        <el-table-column prop="joinCount" label="匹配中" width="90" />
        <el-table-column label="操作" min-width="290">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" type="primary" plain @click="edit(row)">内容配置</el-button>
              <el-button v-if="row.status !== 'PUBLISHED'" size="small" type="success" plain
                         @click="publish(row)">发布</el-button>
              <el-button v-else size="small" type="warning" plain @click="offline(row)">下架</el-button>
              <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !scripts.length" class="empty-state">暂无副本，点击右上角新增副本。</div>
    </div>

    <el-dialog v-model="dialogVisible" title="新增副本" width="620px">
      <el-form :model="form" label-position="top">
        <el-form-item label="副本名称">
          <el-input v-model="form.title" maxlength="128" placeholder="例如 午夜档案" />
        </el-form-item>
        <el-form-item label="副本简介">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="案件背景">
          <el-input v-model="form.story" type="textarea" :rows="4" />
        </el-form-item>
        <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 14px">
          <el-form-item label="难度">
            <el-select v-model="form.difficulty" style="width: 100%">
              <el-option label="简单" value="EASY" />
              <el-option label="中等" value="MEDIUM" />
              <el-option label="困难" value="HARD" />
            </el-select>
          </el-form-item>
          <el-form-item label="最少人数">
            <el-input-number v-model="form.minPlayers" :min="2" :max="12" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最大人数">
            <el-input-number v-model="form.maxPlayers" :min="2" :max="12" style="width: 100%" />
          </el-form-item>
          <el-form-item label="开局人数">
            <el-input-number v-model="form.requiredPlayers" :min="1" :max="12" style="width: 100%" />
          </el-form-item>
          <el-form-item label="预计时长（分钟）">
            <el-input-number v-model="form.estimatedMinutes" :min="1" :max="600" style="width: 100%" />
          </el-form-item>
          <el-form-item label="封面地址">
            <el-input v-model="form.coverImage" placeholder="可选" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="create">创建并进入配置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api'

const router = useRouter()
const scripts = ref([])
const loading = ref(true)
const saving = ref(false)
const dialogVisible = ref(false)
const form = reactive({
  title: '',
  description: '',
  story: '',
  difficulty: 'MEDIUM',
  minPlayers: 2,
  maxPlayers: 4,
  requiredPlayers: 4,
  estimatedMinutes: 60,
  coverImage: ''
})

function statusType(status) {
  return { DRAFT: 'info', PUBLISHED: 'success', OFFLINE: 'warning' }[status] || 'info'
}

function statusText(status) {
  return { DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下架' }[status] || status
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/api/admin/scripts')
    scripts.value = res.data || []
  } catch (e) {
    scripts.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    title: '', description: '', story: '', difficulty: 'MEDIUM',
    minPlayers: 2, maxPlayers: 4, requiredPlayers: 4, estimatedMinutes: 60, coverImage: ''
  })
  dialogVisible.value = true
}

async function create() {
  if (!form.title) {
    ElMessage.warning('请填写副本名称')
    return
  }
  saving.value = true
  try {
    const res = await api.post('/api/admin/scripts', { ...form })
    ElMessage.success('副本已创建，请继续配置角色、阶段、线索与答案')
    dialogVisible.value = false
    router.push(`/admin/scripts/${res.data.id}`)
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

function edit(row) {
  router.push(`/admin/scripts/${row.id}`)
}

async function publish(row) {
  try {
    await ElMessageBox.confirm('发布前会校验开局人数、角色数量、公开线索、关键线索与标准答案。确认发布？', '发布副本')
    await api.post(`/api/admin/scripts/${row.id}/publish`)
    ElMessage.success('副本已发布')
    await load()
  } catch (e) {
    // 取消或拦截器已提示
  }
}

async function offline(row) {
  try {
    await ElMessageBox.confirm('下架后新玩家无法加入匹配，已开始的游戏不受影响。确认下架？', '下架副本')
    await api.post(`/api/admin/scripts/${row.id}/offline`)
    ElMessage.success('副本已下架')
    await load()
  } catch (e) {
    // 取消或拦截器已提示
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('删除副本会同时删除角色、阶段、线索、答案与房间记录，确认删除？', '删除副本',
      { type: 'warning' })
    await api.delete(`/api/admin/scripts/${row.id}`)
    ElMessage.success('副本已删除')
    await load()
  } catch (e) {
    // 取消或拦截器已提示
  }
}

onMounted(load)
</script>
