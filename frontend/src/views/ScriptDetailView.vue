<template>
  <div v-if="script">
    <div class="page-head">
      <div>
        <el-button text class="muted" style="padding-left: 0" @click="router.back()">← 返回大厅</el-button>
        <h1 class="page-title">{{ script.title }}</h1>
        <p class="page-sub">
          难度 {{ difficultyText(script.difficulty) }} · {{ script.requiredPlayers }} 人开局 ·
          预计 {{ script.estimatedMinutes }} 分钟 · {{ script.clueCount }} 条线索
        </p>
      </div>
      <el-button type="primary" size="large" @click="join">⚔ 开始匹配</el-button>
    </div>

    <div class="detail-grid">
      <div>
        <div class="panel">
          <h3 class="panel-title">副本简介</h3>
          <p class="clue-content">{{ script.description }}</p>
          <h4 style="margin: 26px 0 12px; font-family: 'Ma Shan Zheng', serif; letter-spacing: 3px; color: #e53e5a; font-weight: 700; font-size: 18px">📜 案件背景</h4>
          <p class="clue-content">{{ script.story }}</p>
        </div>

        <div class="panel">
          <h3 class="panel-title">阶段解锁</h3>
          <div v-for="stage in script.stages" :key="stage.id" class="timeline-item">
            <strong style="letter-spacing: 2px">阶段 {{ stage.stageOrder }} · {{ stage.stageName }}</strong>
            <p class="clue-content">{{ stage.stageDescription }}</p>
            <span class="muted" style="font-size: 12px; letter-spacing: 1px">
              解锁规则：{{ stage.unlockRule || '按房间进度解锁' }}
            </span>
          </div>
          <div v-if="!script.stages.length" class="empty-state">管理员尚未配置阶段</div>
        </div>

        <div class="panel">
          <h3 class="panel-title">人物角色</h3>
          <div class="grid-cards" style="grid-template-columns: repeat(auto-fill, minmax(220px, 1fr))">
            <div v-for="c in script.characters" :key="c.id" class="clue-card">
              <div class="clue-title">
                🎭 {{ c.characterName }}
                <el-tag v-if="c.isHidden" size="small" type="warning" effect="dark">隐秘角色</el-tag>
              </div>
              <p class="clue-content">{{ c.description }}</p>
            </div>
          </div>
          <div v-if="!script.characters.length" class="empty-state">管理员尚未配置角色</div>
        </div>
      </div>

      <div>
        <div class="panel">
          <h3 class="panel-title">匹配规则</h3>
          <div class="stat-row" style="grid-template-columns: 1fr 1fr">
            <div class="stat-box"><strong>{{ script.requiredPlayers }}</strong><span>开局人数</span></div>
            <div class="stat-box"><strong>{{ script.characterCount || script.characters.length }}</strong><span>角色数量</span></div>
          </div>
          <ul class="muted" style="line-height: 2.2; font-size: 13px; padding-left: 20px; margin: 20px 0">
            <li>同一玩家不能重复加入同一副本匹配</li>
            <li>人数达到开局要求后自动创建房间</li>
            <li>角色由后端随机分配，无法自行选择</li>
            <li>线索按角色与阶段过滤，由后端校验</li>
          </ul>
          <el-button type="primary" style="width: 100%" @click="join">⚔ 加入匹配队列</el-button>
        </div>

        <div class="panel">
          <h3 class="panel-title">证据链机制</h3>
          <p class="clue-content">
            提交结论时可以引用线索，系统会检查支持、矛盾、间接、关键与误导关系，
            并结合时间线冲突进行客观评分，最终展示分数明细与案件复盘。
          </p>
          <div class="meta-row" style="margin-top: 16px">
            <span class="chip">SUPPORT 支持</span>
            <span class="chip red">CONTRADICT 矛盾</span>
            <span class="chip">INDIRECT 间接</span>
            <span class="chip gold">KEY 关键</span>
            <span class="chip">MISLEADING 误导</span>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div v-else v-loading="true" class="panel" style="height: 260px"></div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const props = defineProps({ id: { type: String, required: true } })
const router = useRouter()
const script = ref(null)

function difficultyText(value) {
  return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[value] || value
}

function join() {
  router.push(`/match/${props.id}`)
}

onMounted(async () => {
  try {
    const res = await api.get(`/api/scripts/${props.id}`)
    script.value = res.data
  } catch (e) {
    script.value = null
  }
})
</script>
