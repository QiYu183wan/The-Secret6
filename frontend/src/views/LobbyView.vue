<template>
  <div>
    <div class="page-head">
      <div>
        <h1 class="page-title">副本大厅</h1>
        <p class="page-sub">已发布副本 {{ scripts.length }} 个 · 选择一场审判，揭开真相的面纱</p>
      </div>
      <el-button :loading="loading" @click="load">🔄 刷新副本</el-button>
    </div>

    <div v-if="loading" v-loading="true" class="panel" style="height: 220px"></div>
    <div v-else-if="!scripts.length" class="panel empty-state">
      ☠ 暂无已发布副本 ☠<br /><br />请等待管理员发布新的剧本。
    </div>
    <div v-else class="grid-cards">
      <div v-for="s in scripts" :key="s.id" class="script-card" @click="open(s)">
        <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 10px">
          <h3>{{ s.title }}</h3>
          <el-tag size="small" effect="dark" :type="difficultyType(s.difficulty)">
            {{ difficultyText(s.difficulty) }}
          </el-tag>
        </div>
        <p class="script-desc">{{ s.description }}</p>
        <div class="meta-row">
          <span class="chip">👥 {{ s.requiredPlayers }} 人开局</span>
          <span class="chip">⏱ {{ s.estimatedMinutes }} 分钟</span>
          <span class="chip">🎭 {{ s.characterCount }} 个角色</span>
          <span class="chip">🔍 {{ s.clueCount }} 条线索</span>
          <span v-if="s.joinCount" class="chip gold">🔥 {{ s.joinCount }} 人匹配中</span>
        </div>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span class="muted" style="font-size: 12px; letter-spacing: 1px">点击查看案件详情 →</span>
          <el-button type="primary" size="small">进入副本</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const scripts = ref([])
const loading = ref(true)

function difficultyText(value) {
  return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[value] || value
}

function difficultyType(value) {
  return { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }[value] || 'info'
}

function open(script) {
  router.push(`/scripts/${script.id}`)
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/api/scripts')
    scripts.value = res.data || []
  } catch (e) {
    scripts.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
