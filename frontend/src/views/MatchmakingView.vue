<template>
  <div style="max-width: 720px; margin: 40px auto 0">
    <div class="panel" style="text-align: center; padding: 50px 30px">
      <h2 class="page-title" style="margin-bottom: 10px">正在匹配</h2>
      <p class="page-sub">{{ script?.title || '副本' }} · 达到 {{ status?.requiredCount || required }} 人后自动开局</p>

      <div style="margin: 40px 0 10px; display: flex; justify-content: center">
        <div class="score-ring" style="width: 120px; height: 120px; font-size: 32px">
          {{ status?.waitingCount || 0 }}/{{ status?.requiredCount || required }}
        </div>
      </div>
      <p class="muted" style="margin: 10px 0 0; font-size: 14px; letter-spacing: 2px">
        {{ status?.status === 'CANCELLED' ? '匹配已取消' : '等待其他玩家加入，页面会自动刷新' }}
      </p>

      <div style="display: flex; gap: 12px; justify-content: center; margin-top: 35px">
        <el-button v-if="status?.status !== 'CANCELLED'" @click="cancel">取消匹配</el-button>
        <el-button v-else type="primary" @click="rejoin">重新加入匹配</el-button>
        <el-button text @click="router.push(`/scripts/${scriptId}`)">返回副本详情</el-button>
      </div>

      <div v-if="status?.status === 'MATCHED'" class="empty-state" style="color: #c41e3a">☠ 匹配成功，正在进入房间 ☠</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const props = defineProps({ scriptId: { type: String, required: true } })
const router = useRouter()
const script = ref(null)
const status = ref(null)
const required = ref(4)
let timer = null

const scriptId = computed(() => Number(props.scriptId))

async function join() {
  const res = await api.post(`/api/matchmaking/${scriptId.value}/join`)
  status.value = res.data
  if (status.value?.status === 'MATCHED' && status.value.roomId) {
    enterRoom(status.value.roomId)
  }
}

async function rejoin() {
  try {
    await join()
  } catch (e) {
    // 拦截器已提示
  }
}

async function cancel() {
  try {
    await api.delete(`/api/matchmaking/${scriptId.value}/cancel`)
    ElMessage.info('已取消匹配')
    status.value = { ...status.value, status: 'CANCELLED' }
  } catch (e) {
    // 拦截器已提示
  }
}

async function poll() {
  try {
    const res = await api.get(`/api/matchmaking/${scriptId.value}/status`)
    status.value = res.data
    if (res.data?.status === 'MATCHED' && res.data.roomId) {
      enterRoom(res.data.roomId)
    }
  } catch (e) {
    // 轮询失败保持页面状态
  }
}

function enterRoom(roomId) {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  ElMessage.success('匹配成功，正在进入房间')
  router.replace(`/rooms/${roomId}`)
}

onMounted(async () => {
  try {
    const res = await api.get(`/api/scripts/${scriptId.value}`)
    script.value = res.data
    required.value = res.data.requiredPlayers
  } catch (e) {
    // 忽略详情加载失败，匹配仍可继续
  }
  try {
    await join()
  } catch (e) {
    // 拦截器已提示
  }
  timer = setInterval(poll, 2000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>
