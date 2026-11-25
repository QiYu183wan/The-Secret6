<template>
  <div>
    <div class="page-head">
      <div>
        <h1 class="page-title">用户账号</h1>
        <p class="page-sub">管理员可禁用或启用账号，禁用后无法登录</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <div class="panel">
      <el-table v-loading="loading" :data="users">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="140" />
        <el-table-column label="角色" width="160">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'info'" effect="dark">
              {{ row.role === 'ADMIN' ? '👑 管理员' : '🎭 普通玩家' }}
            </el-tag>
            <el-switch
              style="margin-left: 10px"
              :model-value="row.role === 'ADMIN'"
              :active-text="'管理员'"
              :inactive-text="'玩家'"
              @change="(value) => toggleRole(row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" effect="plain">
              {{ row.status === 'ACTIVE' ? '正常' : '已禁用' }}
            </el-tag>
            <el-switch
              style="margin-left: 10px"
              :model-value="row.status === 'ACTIVE'"
              @change="(value) => toggleStatus(row, value)"
            />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const users = ref([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const res = await api.get('/api/admin/users')
    users.value = res.data || []
  } catch (e) {
    users.value = []
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row, enabled) {
  const status = enabled ? 'ACTIVE' : 'DISABLED'
  try {
    await api.put(`/api/admin/users/${row.id}/status`, { status })
    row.status = status
    ElMessage.success(enabled ? '账号已启用' : '账号已禁用')
  } catch (e) {
    await load()
  }
}

async function toggleRole(row, isAdmin) {
  const role = isAdmin ? 'ADMIN' : 'PLAYER'
  const action = isAdmin ? '升为管理员' : '降为普通玩家'
  try {
    await api.put(`/api/admin/users/${row.id}/role`, { status: role })
    row.role = role
    ElMessage.success(`${row.username} 已${action}`)
  } catch (e) {
    await load()
  }
}

onMounted(load)
</script>
