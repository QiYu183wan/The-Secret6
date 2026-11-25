<template>
  <div v-if="room">
    <div class="page-head">
      <div>
        <el-button text class="muted" style="padding-left: 0" @click="router.push('/lobby')">← 返回大厅</el-button>
        <h1 class="page-title">{{ room.scriptTitle }}</h1>
        <p class="page-sub">
          房间 #{{ room.id }} · {{ statusText }} ·
          当前阶段 {{ room.currentStageOrder || '-' }} · {{ room.currentStageName || '未开始' }}
        </p>
      </div>
      <div class="meta-row">
        <span class="chip">👥 {{ room.players.length }} 名玩家</span>
        <span class="chip" :class="{ gold: finished, red: finished }">{{ statusText }}</span>
        <el-button v-if="finished" type="primary" @click="tab = 'result'">查看结算 ☠</el-button>
      </div>
    </div>

    <div class="detail-grid">
      <div>
        <div class="panel" v-if="character">
          <h3 class="panel-title">我的角色</h3>
          <div class="clue-title" style="font-size: 20px">
            🎭 {{ character.characterName }}
            <el-tag v-if="character.hidden" size="small" type="warning" effect="dark" style="margin-left: 10px">隐秘角色</el-tag>
          </div>
          <p class="clue-content">{{ character.description }}</p>
          <p v-if="character.goal" class="clue-content" style="margin-top: 12px; padding: 14px; background: rgba(196, 30, 58, 0.1); border-left: 3px solid #c41e3a">
            <span class="red" style="font-weight: 700; letter-spacing: 2px">角色目标：</span><span style="margin-left: 8px">{{ character.goal }}</span>
          </p>
        </div>

        <div class="panel" v-if="stage">
          <h3 class="panel-title">阶段进度</h3>
          <div v-for="item in stage.stages" :key="item.id" class="timeline-item">
            <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 8px">
              <strong style="letter-spacing: 2px">阶段 {{ item.stageOrder }} · {{ item.stageName }}</strong>
              <el-tag size="small" :type="item.open ? 'success' : 'info'" effect="plain">
                {{ item.open ? '已开放' : '未解锁' }}
              </el-tag>
            </div>
            <p class="clue-content">{{ item.stageDescription }}</p>
          </div>
        </div>
      </div>

      <div>
        <div class="panel">
          <h3 class="panel-title">房间玩家</h3>
          <div v-for="p in room.players" :key="p.userId"
               style="display: flex; justify-content: space-between; padding: 11px 0; border-bottom: 1px dashed #3a2a45">
            <span style="font-weight: 600">{{ p.nickname }}</span>
            <span class="muted" style="letter-spacing: 1px">{{ p.characterName }}</span>
          </div>
          <p class="muted" style="font-size: 12px; margin: 14px 0 0; letter-spacing: 1px">角色由后端随机分配，不可自行选择</p>
        </div>
        <div class="panel">
          <h3 class="panel-title">推理状态</h3>
          <p class="clue-content" style="line-height: 2.2">
            你的提交：<span :class="{ gold: mySubmitted, red: !mySubmitted }" style="font-weight: 700">{{ mySubmitted ? '✓ 已提交' : '✗ 未提交' }}</span><br />
            全员提交：<span :class="{ gold: room.allSubmitted, red: !room.allSubmitted }" style="font-weight: 700">{{ room.allSubmitted ? '✓ 已完成' : '… 等待中' }}</span>
          </p>
          <p class="muted" style="font-size: 12px; margin-top: 14px; letter-spacing: 1px">
            所有玩家提交后自动结算，页面每 5 秒刷新一次。
          </p>
        </div>
      </div>
    </div>

    <div class="panel" style="margin-top: 24px">
      <el-tabs v-model="tab">
        <el-tab-pane label="🔍 线索" name="clues">
          <div class="clue-list" style="margin-top: 8px">
            <div v-for="clue in clues" :key="clue.id"
                 class="clue-card"
                 :class="{ key: clue.isKeyClue === 1, misleading: clue.isMisleading === 1 }">
              <div class="clue-title">
                {{ clue.title }}
                <el-tag size="small" effect="plain">{{ clue.stageName || '通用' }}</el-tag>
                <el-tag v-if="clue.isKeyClue === 1" size="small" type="warning" effect="dark">★ 关键</el-tag>
                <el-tag v-if="clue.isMisleading === 1" size="small" type="danger" effect="dark">⚠ 误导</el-tag>
              </div>
              <p class="clue-content">{{ clue.content }}</p>
              <div class="meta-row" style="margin-top: 14px">
                <span v-if="clue.eventTime" class="chip">⏱ 时间 {{ clue.eventTime }}</span>
                <span v-if="clue.location" class="chip">📍 地点 {{ clue.location }}</span>
                <span class="chip">可信度 {{ clue.credibility }}/10</span>
                <span class="chip">{{ visibilityText(clue.visibilityType) }}</span>
              </div>
            </div>
            <div v-if="!clues.length" class="empty-state">当前阶段暂无可查看线索</div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="✍ 提交推理" name="submit">
          <div v-if="finished" class="empty-state">☠ 游戏已结束，请查看结算结果与案件复盘 ☠</div>
          <div v-else-if="mySubmitted" class="empty-state" style="line-height: 2">
            你已提交最终推理<span v-if="room.myScore !== null">，本次得分 <span class="gold" style="font-size: 20px">{{ room.myScore }}</span></span><br />等待其他玩家提交后自动结算。
          </div>
          <el-form v-else label-position="top" style="margin-top: 12px">
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px">
              <el-form-item label="人物">
                <el-select v-model="form.characterAnswer" filterable allow-create
                           placeholder="选择或输入人物" style="width: 100%">
                  <el-option v-for="name in room.characterNames" :key="name" :label="name" :value="name" />
                </el-select>
              </el-form-item>
              <el-form-item label="时间">
                <el-input v-model="form.timeAnswer" placeholder="例如 11月15日凌晨00:42" />
              </el-form-item>
              <el-form-item label="地点">
                <el-input v-model="form.locationAnswer" placeholder="例如 档案室B区-03-12" />
              </el-form-item>
              <el-form-item label="行为 / 作案方式">
                <el-input v-model="form.actionAnswer" placeholder="描述档案被带离的方式" />
              </el-form-item>
            </div>
            <el-form-item label="推理说明（可选）">
              <el-input v-model="form.explanation" type="textarea" :rows="3"
                        placeholder="可以写下你的推理过程" />
            </el-form-item>
            <el-form-item label="引用线索">
              <div class="clue-list" style="width: 100%">
                <div v-for="clue in clues" :key="clue.id" class="clue-card" style="padding: 14px 16px">
                  <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap">
                    <el-checkbox v-model="refState[clue.id].checked">{{ clue.title }}</el-checkbox>
                    <el-select v-model="refState[clue.id].relation" size="small" style="width: 140px"
                               :disabled="!refState[clue.id].checked">
                      <el-option label="支持" value="SUPPORT" />
                      <el-option label="矛盾" value="CONTRADICT" />
                      <el-option label="间接" value="INDIRECT" />
                    </el-select>
                    <el-tag v-if="clue.isKeyClue === 1" size="small" type="warning">关键</el-tag>
                    <el-tag v-if="clue.isMisleading === 1" size="small" type="danger">误导</el-tag>
                  </div>
                </div>
                <div v-if="!clues.length" class="muted" style="font-size: 12px">暂无可引用线索</div>
              </div>
            </el-form-item>
            <el-button type="primary" :loading="submitting" @click="submit">⚔ 提交最终推理</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="🔗 证据链" name="evidence">
          <div v-if="evidence">
            <div class="clue-card" style="border-color: #c41e3a; margin-top: 8px">
              <div class="field-label">当前推理结论</div>
              <p class="clue-content" style="font-size: 15px; letter-spacing: 1px">{{ evidence.conclusion }}</p>
              <div class="meta-row" style="margin-top: 14px">
                <span v-for="hint in evidence.hints" :key="hint" class="chip red">{{ hint }}</span>
              </div>
            </div>

            <div class="grid-cards" style="margin-top: 18px; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr))">
              <div v-for="group in evidenceGroups" :key="group.key" class="clue-card">
                <div class="clue-title">
                  {{ group.label }}
                  <el-tag size="small" effect="plain">{{ group.items.length }}</el-tag>
                </div>
                <div v-if="!group.items.length" class="muted" style="font-size: 12px">暂无</div>
                <div v-for="item in group.items" :key="group.key + '-' + item.id"
                     style="padding: 7px 0; border-bottom: 1px dashed #3a2a45; font-size: 13px">
                  <span :class="{ gold: evidence.referencedClueIds.includes(item.id) }">{{ item.title }}</span>
                  <el-tag v-if="evidence.referencedClueIds.includes(item.id)" size="small" type="success"
                          style="margin-left: 6px">已引用</el-tag>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">证据链加载中</div>
        </el-tab-pane>

        <el-tab-pane label="☠ 结算" name="result">
          <div v-if="!result" class="empty-state">游戏结束后显示结算结果。</div>
          <div v-else style="margin-top: 12px">
            <div class="result-hero">
              <div class="score-ring">{{ result.myScore }}</div>
              <div>
                <h2 style="margin: 0 0 10px; font-family: 'Ma Shan Zheng', serif; font-size: 28px; letter-spacing: 4px; background: linear-gradient(135deg, #e53e5a, #ffb8c2); -webkit-background-clip: text; background-clip: text; color: transparent">
                  {{ resultLabel(result.result) }}
                </h2>
                <p class="muted" style="margin: 0 0 12px">
                  副本：{{ result.scriptTitle }} · 角色：{{ character?.characterName }}
                </p>
                <el-tag v-if="result.hiddenGoal !== null" type="warning" effect="dark">
                  {{ result.hiddenGoal ? '✦ 隐秘目标已达成' : '✗ 隐秘目标未达成' }}
                </el-tag>
              </div>
            </div>

            <el-table :data="result.players" style="margin-top: 28px">
              <el-table-column type="index" label="#" width="60" />
              <el-table-column prop="nickname" label="玩家" />
              <el-table-column prop="characterName" label="角色" />
              <el-table-column prop="score" label="得分" width="100">
                <template #default="{ row }">
                  <span :style="{ color: row.score >= 60 ? '#d4a843' : '#c41e3a', fontWeight: 700, fontSize: '16px' }">{{ row.score }}</span>
                </template>
              </el-table-column>
              <el-table-column label="结果" width="140">
                <template #default="{ row }">
                  <el-tag v-if="row.result === 'SUCCESS'" type="success" effect="dark">✦ 推理成功</el-tag>
                  <el-tag v-else-if="row.result === 'PARTIAL'" type="warning" effect="dark">⚠ 部分正确</el-tag>
                  <el-tag v-else type="danger" effect="dark">☠ 推理失败</el-tag>
                </template>
              </el-table-column>
            </el-table>

            <div v-if="resultDetail" class="panel" style="margin-top: 24px">
              <h3 class="panel-title">得分明细</h3>
              <div class="stat-row">
                <div class="stat-box"><strong>{{ resultDetail.baseScore }}</strong><span>基础得分</span></div>
                <div class="stat-box"><strong>{{ resultDetail.supportScore }}</strong><span>支持线索</span></div>
                <div class="stat-box"><strong>{{ resultDetail.keyBonus }}</strong><span>关键线索加分</span></div>
                <div class="stat-box" style="border-color: #c41e3a"><strong style="color: #c41e3a">-{{ resultDetail.contradictPenalty }}</strong><span>矛盾线索扣分</span></div>
                <div class="stat-box" style="border-color: #c41e3a"><strong style="color: #c41e3a">-{{ resultDetail.misleadingPenalty }}</strong><span>误导线索扣分</span></div>
                <div class="stat-box" style="border-color: #c41e3a"><strong style="color: #c41e3a">-{{ resultDetail.missingKeyPenalty }}</strong><span>缺失关键线索</span></div>
                <div class="stat-box" style="border-color: #c41e3a"><strong style="color: #c41e3a">-{{ resultDetail.timeConflictPenalty }}</strong><span>时间线冲突</span></div>
                <div class="stat-box" style="border-color: #d4a843; box-shadow: 0 0 20px rgba(212, 168, 67, 0.2)"><strong style="color: #d4a843">{{ resultDetail.totalScore }}</strong><span>最终得分</span></div>
              </div>
              <ul style="margin: 22px 0 0; line-height: 2.2; color: #b8a8b8; padding-left: 20px">
                <li v-for="msg in resultDetail.messages" :key="msg" style="margin-bottom: 4px">{{ msg }}</li>
              </ul>
              <div style="margin-top: 18px">
                <div class="field-label">引用的支持线索</div>
                <div class="meta-row">
                  <span v-for="title in resultDetail.supportClues" :key="'s-' + title" class="chip gold">{{ title }}</span>
                  <span v-if="!resultDetail.supportClues?.length" class="muted" style="font-size: 12px">无</span>
                </div>
              </div>
              <div style="margin-top: 16px">
                <div class="field-label">缺失的关键线索</div>
                <div class="meta-row">
                  <span v-for="title in resultDetail.missingKeyClues" :key="'m-' + title" class="chip red">{{ title }}</span>
                  <span v-if="!resultDetail.missingKeyClues?.length" class="muted" style="font-size: 12px">无</span>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="📜 复盘" name="replay">
          <div v-if="!replay" class="empty-state">游戏结束后可查看案件复盘。</div>
          <div v-else style="margin-top: 12px">
            <div class="panel" style="margin-top: 0">
              <h3 class="panel-title">标准答案</h3>
              <div class="stat-row" style="grid-template-columns: repeat(2, 1fr)">
                <div class="stat-box"><strong style="font-size: 18px">{{ replay.standardAnswer.characterAnswer }}</strong><span>人物</span></div>
                <div class="stat-box"><strong style="font-size: 18px">{{ replay.standardAnswer.timeAnswer }}</strong><span>时间</span></div>
                <div class="stat-box"><strong style="font-size: 18px">{{ replay.standardAnswer.locationAnswer }}</strong><span>地点</span></div>
                <div class="stat-box"><strong style="font-size: 16px">{{ replay.standardAnswer.actionAnswer }}</strong><span>行为 / 方式</span></div>
              </div>
            </div>

            <div class="panel">
              <h3 class="panel-title">案件时间线</h3>
              <div v-for="(line, index) in replay.timeline" :key="index" class="timeline-item">
                <span class="clue-content">{{ line }}</span>
              </div>
              <div v-if="!replay.timeline.length" class="empty-state">暂无可用于时间线的线索</div>
            </div>

            <div class="panel">
              <h3 class="panel-title">证据关系</h3>
              <el-table :data="replay.relations">
                <el-table-column prop="clueTitle" label="线索" min-width="180" />
                <el-table-column prop="answerField" label="对应字段" width="120" />
                <el-table-column prop="relationType" label="关系" width="120" />
                <el-table-column prop="score" label="分值" width="90" />
              </el-table>
            </div>

            <div class="panel">
              <h3 class="panel-title">全部线索</h3>
              <div class="clue-list">
                <div v-for="clue in replay.clues" :key="clue.id" class="clue-card" :class="{ key: clue.isKeyClue === 1 }">
                  <div class="clue-title">
                    {{ clue.title }}
                    <el-tag v-if="clue.isKeyClue === 1" size="small" type="warning" effect="dark">★ 关键</el-tag>
                    <el-tag v-if="clue.isMisleading === 1" size="small" type="danger" effect="dark">⚠ 误导</el-tag>
                    <el-tag size="small" effect="plain">{{ clue.stageName || '通用' }}</el-tag>
                  </div>
                  <p class="clue-content">{{ clue.content }}</p>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
  <div v-else v-loading="true" class="panel" style="height: 260px"></div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const props = defineProps({ id: { type: String, required: true } })
const router = useRouter()
const roomId = computed(() => Number(props.id))

const room = ref(null)
const character = ref(null)
const stage = ref(null)
const clues = ref([])
const evidence = ref(null)
const result = ref(null)
const replay = ref(null)
const tab = ref('clues')
const submitting = ref(false)
const refState = reactive({})
let timer = null

const form = reactive({
  characterAnswer: '',
  timeAnswer: '',
  locationAnswer: '',
  actionAnswer: '',
  explanation: ''
})

const finished = computed(() => room.value?.status === 'FINISHED')
const mySubmitted = computed(() => room.value?.mySubmitted)

const statusText = computed(() => ({
  WAITING: '等待开始',
  IN_PROGRESS: '进行中',
  FINISHED: '已结束',
  ABANDONED: '已中止'
}[room.value?.status] || room.value?.status))

const resultDetail = computed(() => {
  if (!result.value?.scoreDetail) {
    return null
  }
  try {
    return JSON.parse(result.value.scoreDetail)
  } catch (e) {
    return null
  }
})

const evidenceGroups = computed(() => {
  if (!evidence.value) {
    return []
  }
  return [
    { key: 'support', label: '✦ 支持结论的线索', items: evidence.value.supportClues || [] },
    { key: 'key', label: '★ 关键线索', items: evidence.value.keyClues || [] },
    { key: 'indirect', label: '… 间接线索', items: evidence.value.indirectClues || [] },
    { key: 'contradict', label: '✗ 矛盾线索', items: evidence.value.contradictClues || [] },
    { key: 'misleading', label: '⚠ 误导线索', items: evidence.value.misleadingClues || [] },
    { key: 'unreferenced', label: '？ 尚未引用的线索', items: evidence.value.unreferencedClues || [] }
  ]
})

function visibilityText(value) {
  return {
    PUBLIC: '公开线索',
    CHARACTER: '角色专属',
    PLAYER: '玩家专属',
    HIDDEN: '阶段解锁'
  }[value] || value
}

function resultLabel(value) {
  return { SUCCESS: '✦ 推理成功', PARTIAL: '⚠ 部分正确', FAIL: '☠ 推理失败' }[value] || value || '-'
}

async function loadRoom() {
  const res = await api.get(`/api/rooms/${roomId.value}`)
  room.value = res.data
}

async function loadDetail() {
  const [characterRes, stageRes, cluesRes, evidenceRes] = await Promise.all([
    api.get(`/api/rooms/${roomId.value}/character`),
    api.get(`/api/rooms/${roomId.value}/stage`),
    api.get(`/api/rooms/${roomId.value}/clues`),
    api.get(`/api/rooms/${roomId.value}/evidence-chain`)
  ])
  character.value = characterRes.data
  stage.value = stageRes.data
  clues.value = cluesRes.data || []
  evidence.value = evidenceRes.data
  clues.value.forEach((clue) => {
    if (!refState[clue.id]) {
      refState[clue.id] = { checked: false, relation: 'SUPPORT' }
    }
  })
}

async function loadResult() {
  const [resultRes, replayRes] = await Promise.all([
    api.get(`/api/rooms/${roomId.value}/result`),
    api.get(`/api/rooms/${roomId.value}/replay`)
  ])
  result.value = resultRes.data
  replay.value = replayRes.data
}

async function refresh() {
  try {
    const wasFinished = finished.value
    await loadRoom()
    if (finished.value) {
      if (!wasFinished) {
        await loadResult()
        tab.value = 'result'
      }
      stopPolling()
    } else {
      await loadDetail()
    }
  } catch (e) {
    // 轮询失败保持当前页面
  }
}

async function submit() {
  if (mySubmitted.value) {
    return
  }
  if (!form.characterAnswer || !form.timeAnswer || !form.locationAnswer || !form.actionAnswer) {
    ElMessage.warning('请完整填写人物、时间、地点和行为')
    return
  }
  const refs = clues.value
    .filter((clue) => refState[clue.id]?.checked)
    .map((clue) => ({ clueId: clue.id, relationType: refState[clue.id].relation }))
  submitting.value = true
  try {
    const res = await api.post(`/api/rooms/${roomId.value}/submission`, { ...form, clues: refs })
    ElMessage.success(`提交成功，本次得分 ${res.data.score}`)
    await loadRoom()
    if (res.data.roomFinished) {
      await loadResult()
      tab.value = 'result'
      stopPolling()
    } else {
      await loadDetail()
      tab.value = 'evidence'
    }
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

function stopPolling() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onMounted(async () => {
  try {
    await loadRoom()
    if (finished.value) {
      await loadDetail()
      await loadResult()
      tab.value = 'result'
    } else {
      await loadDetail()
      timer = setInterval(refresh, 5000)
    }
  } catch (e) {
    // 拦截器已提示
  }
})

onUnmounted(stopPolling)
</script>
