<template>
  <div>
    <div class="page-head">
      <div>
        <h1 class="page-title">房间记录</h1>
        <p class="page-sub">最近 200 个游戏房间，含进行中、已结束与已中止状态</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <div class="panel">
      <el-table v-loading="loading" :data="rooms">
        <el-table-column prop="id" label="房间" width="90" />
        <el-table-column prop="scriptTitle" label="副本" min-width="180" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="dark">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="playerCount" label="玩家数" width="100" />
        <el-table-column label="创建时间" width="190">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="190">
          <template #default="{ row }">{{ formatTime(row.finishTime) }}</template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !rooms.length" class="empty-state">暂无房间记录</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../../api'

const rooms = ref([])
const loading = ref(true)

function statusText(status) {
  return { WAITING: '等待开始', IN_PROGRESS: '进行中', FINISHED: '已结束', ABANDONED: '已中止' }[status] || status
}

function statusType(status) {
  return { WAITING: 'info', IN_PROGRESS: 'warning', FINISHED: 'success', ABANDONED: 'danger' }[status] || 'info'
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/api/admin/rooms')
    rooms.value = res.data || []
  } catch (e) {
    rooms.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
