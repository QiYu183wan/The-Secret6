<template>
  <div>
    <div class="page-head">
      <div>
        <h1 class="page-title">推理记录</h1>
        <p class="page-sub">历史结算结果保存在 MySQL，可随时回看房间与复盘</p>
      </div>
      <el-button :loading="loading" @click="load">🔄 刷新</el-button>
    </div>

    <div class="panel">
      <el-table v-loading="loading" :data="items">
        <el-table-column prop="scriptTitle" label="副本" min-width="160" />
        <el-table-column prop="characterName" label="角色" width="140" />
        <el-table-column prop="score" label="得分" width="90">
          <template #default="{ row }">
            <span :style="{ color: row.score >= 60 ? '#d4a843' : '#c41e3a', fontWeight: 700 }">{{ row.score }}</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.result === 'SUCCESS'" type="success" effect="dark">✦ 推理成功</el-tag>
            <el-tag v-else-if="row.result === 'PARTIAL'" type="warning" effect="dark">⚠ 部分正确</el-tag>
            <el-tag v-else type="danger" effect="dark">☠ 推理失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结束时间" width="180">
          <template #default="{ row }">{{ formatTime(row.finishTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openRoom(row.roomId)">查看结算</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !items.length" class="empty-state">
        ☠ 尚无推理记录 ☠<br /><br />完成一局副本后即可在这里查看。
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const items = ref([])
const loading = ref(true)

function resultLabel(value) {
  return { SUCCESS: '推理成功', PARTIAL: '部分正确', FAIL: '推理失败' }[value] || value || '-'
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

function openRoom(roomId) {
  router.push(`/rooms/${roomId}`)
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/api/user/history')
    items.value = res.data || []
  } catch (e) {
    items.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
