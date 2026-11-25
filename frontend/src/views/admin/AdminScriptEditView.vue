<template>
  <div v-if="script">
    <div class="page-head">
      <div>
        <el-button text class="muted" style="padding-left: 0" @click="router.push('/admin/scripts')">返回副本列表</el-button>
        <h1 class="page-title">{{ script.title }}</h1>
        <p class="page-sub">
          状态：{{ statusText(script.status) }} · {{ script.requiredPlayers }} 人开局 ·
          {{ characters.length }} 个角色 · {{ clues.length }} 条线索
        </p>
      </div>
      <div style="display: flex; gap: 10px">
        <el-button v-if="script.status !== 'PUBLISHED'" type="success" @click="publish">发布副本</el-button>
        <el-button v-else type="warning" @click="offline">下架副本</el-button>
      </div>
    </div>

    <div class="panel">
      <el-tabs v-model="tab" @tab-change="onTabChange">
        <el-tab-pane label="基本信息" name="basic">
          <el-form :model="basic" label-position="top" style="margin-top: 10px">
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px">
              <el-form-item label="副本名称">
                <el-input v-model="basic.title" />
              </el-form-item>
              <el-form-item label="难度">
                <el-select v-model="basic.difficulty" style="width: 100%">
                  <el-option label="简单" value="EASY" />
                  <el-option label="中等" value="MEDIUM" />
                  <el-option label="困难" value="HARD" />
                </el-select>
              </el-form-item>
            </div>
            <el-form-item label="副本简介">
              <el-input v-model="basic.description" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="案件背景">
              <el-input v-model="basic.story" type="textarea" :rows="4" />
            </el-form-item>
            <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px">
              <el-form-item label="最少人数">
                <el-input-number v-model="basic.minPlayers" :min="1" :max="12" style="width: 100%" />
              </el-form-item>
              <el-form-item label="最大人数">
                <el-input-number v-model="basic.maxPlayers" :min="1" :max="12" style="width: 100%" />
              </el-form-item>
              <el-form-item label="开局人数">
                <el-input-number v-model="basic.requiredPlayers" :min="1" :max="12" style="width: 100%" />
              </el-form-item>
              <el-form-item label="预计分钟">
                <el-input-number v-model="basic.estimatedMinutes" :min="1" :max="600" style="width: 100%" />
              </el-form-item>
            </div>
            <el-form-item label="封面地址">
              <el-input v-model="basic.coverImage" placeholder="可选" />
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="saveBasic">保存基本信息</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="角色配置" name="characters">
          <div style="display: flex; justify-content: space-between; margin: 10px 0 14px">
            <span class="muted" style="font-size: 13px">发布校验要求角色数量与开局人数一致</span>
            <el-button type="primary" size="small" @click="openCharacter()">新增角色</el-button>
          </div>
          <el-table :data="characters">
            <el-table-column prop="sortOrder" label="排序" width="80" />
            <el-table-column prop="characterName" label="角色名称" width="140" />
            <el-table-column prop="description" label="角色描述" min-width="200" />
            <el-table-column prop="characterGoal" label="角色目标" min-width="180" />
            <el-table-column label="隐秘" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.isHidden === 1" type="warning" size="small">隐秘</el-tag>
                <span v-else class="muted">否</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" plain @click="openCharacter(row)">编辑</el-button>
                <el-button size="small" type="danger" plain @click="removeCharacter(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="阶段配置" name="stages">
          <div style="display: flex; justify-content: flex-end; margin: 10px 0 14px">
            <el-button type="primary" size="small" @click="openStage()">新增阶段</el-button>
          </div>
          <el-table :data="stages">
            <el-table-column prop="stageOrder" label="顺序" width="80" />
            <el-table-column prop="stageName" label="阶段名称" width="150" />
            <el-table-column prop="stageDescription" label="阶段说明" min-width="220" />
            <el-table-column prop="unlockRule" label="解锁规则" min-width="160" />
            <el-table-column prop="timeoutMinutes" label="等待分钟" width="100" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" plain @click="openStage(row)">编辑</el-button>
                <el-button size="small" type="danger" plain @click="removeStage(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="线索配置" name="clues">
          <div style="display: flex; justify-content: flex-end; margin: 10px 0 14px">
            <el-button type="primary" size="small" @click="openClue()">新增线索</el-button>
          </div>
          <el-table :data="clues">
            <el-table-column prop="sortOrder" label="排序" width="70" />
            <el-table-column prop="title" label="线索标题" width="160" />
            <el-table-column prop="content" label="内容" min-width="220" />
            <el-table-column prop="stageName" label="阶段" width="100" />
            <el-table-column label="可见性" width="110">
              <template #default="{ row }">{{ visibilityText(row.visibilityType) }}</template>
            </el-table-column>
            <el-table-column prop="eventTime" label="事件时间" width="150" />
            <el-table-column prop="location" label="地点" width="130" />
            <el-table-column label="标记" width="140">
              <template #default="{ row }">
                <el-tag v-if="row.isKeyClue === 1" size="small" type="warning">关键</el-tag>
                <el-tag v-if="row.isMisleading === 1" size="small" type="danger" style="margin-left: 4px">误导</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" plain @click="openClue(row)">编辑</el-button>
                <el-button size="small" type="danger" plain @click="removeClue(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="标准答案" name="answer">
          <el-alert type="warning" :closable="false" show-icon style="margin: 10px 0 18px"
                    title="标准答案与评分规则仅后台可见，玩家接口不会返回该数据。" />
          <el-form :model="answerForm" label-position="top">
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px">
              <el-form-item label="人物答案">
                <el-input v-model="answerForm.characterAnswer" />
              </el-form-item>
              <el-form-item label="时间答案">
                <el-input v-model="answerForm.timeAnswer" />
              </el-form-item>
              <el-form-item label="地点答案">
                <el-input v-model="answerForm.locationAnswer" />
              </el-form-item>
              <el-form-item label="行为 / 作案方式">
                <el-input v-model="answerForm.actionAnswer" />
              </el-form-item>
            </div>
            <el-form-item label="评分规则 JSON">
              <el-input v-model="answerForm.scoringRuleJson" type="textarea" :rows="6" />
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="saveAnswer">保存标准答案</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="证据关系" name="relations">
          <div style="display: flex; justify-content: space-between; margin: 10px 0 14px">
            <span class="muted" style="font-size: 13px">关系类型：SUPPORT 支持、CONTRADICT 矛盾、INDIRECT 间接、KEY 关键、MISLEADING 误导</span>
            <el-button type="primary" size="small" @click="openRelation()">新增关系</el-button>
          </div>
          <el-table :data="relations">
            <el-table-column prop="clueTitle" label="线索" min-width="180" />
            <el-table-column prop="answerField" label="答案字段" width="120" />
            <el-table-column prop="relationType" label="关系类型" width="130" />
            <el-table-column prop="score" label="分值" width="90" />
            <el-table-column label="操作" width="110">
              <template #default="{ row }">
                <el-button size="small" type="danger" plain @click="removeRelation(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="副本统计" name="statistics">
          <div v-if="statistics" class="stat-row" style="margin-top: 12px">
            <div class="stat-box"><strong>{{ statistics.rooms }}</strong><span>房间总数</span></div>
            <div class="stat-box"><strong>{{ statistics.finishedRooms }}</strong><span>已结算房间</span></div>
            <div class="stat-box"><strong>{{ statistics.players }}</strong><span>参与玩家</span></div>
            <div class="stat-box"><strong>{{ statistics.averageScore }}</strong><span>平均得分</span></div>
          </div>
          <div v-else class="empty-state">切换到本页后自动加载统计数据</div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="characterDialog" :title="characterForm.id ? '编辑角色' : '新增角色'" width="560px">
      <el-form :model="characterForm" label-position="top">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 14px">
          <el-form-item label="角色名称"><el-input v-model="characterForm.characterName" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="characterForm.sortOrder" :min="0" style="width: 100%" /></el-form-item>
        </div>
        <el-form-item label="角色描述"><el-input v-model="characterForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="角色目标"><el-input v-model="characterForm.characterGoal" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="是否隐秘角色">
          <el-switch v-model="characterForm.isHidden" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="characterDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCharacter">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stageDialog" :title="stageForm.id ? '编辑阶段' : '新增阶段'" width="560px">
      <el-form :model="stageForm" label-position="top">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 14px">
          <el-form-item label="阶段名称"><el-input v-model="stageForm.stageName" /></el-form-item>
          <el-form-item label="阶段顺序"><el-input-number v-model="stageForm.stageOrder" :min="1" style="width: 100%" /></el-form-item>
        </div>
        <el-form-item label="阶段说明"><el-input v-model="stageForm.stageDescription" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="解锁规则 JSON">
          <el-input v-model="stageForm.unlockRule" placeholder='{"type":"START"}' />
        </el-form-item>
        <el-form-item label="最长等待时间（分钟）">
          <el-input-number v-model="stageForm.timeoutMinutes" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stageDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveStage">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="clueDialog" :title="clueForm.id ? '编辑线索' : '新增线索'" width="640px">
      <el-form :model="clueForm" label-position="top">
        <el-form-item label="线索标题"><el-input v-model="clueForm.title" /></el-form-item>
        <el-form-item label="线索内容"><el-input v-model="clueForm.content" type="textarea" :rows="3" /></el-form-item>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 14px">
          <el-form-item label="所属阶段">
            <el-select v-model="clueForm.stageId" clearable placeholder="通用线索" style="width: 100%">
              <el-option v-for="stage in stages" :key="stage.id" :label="stage.stageName" :value="stage.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="可见范围">
            <el-select v-model="clueForm.visibilityType" style="width: 100%">
              <el-option label="公开" value="PUBLIC" />
              <el-option label="指定角色" value="CHARACTER" />
              <el-option label="隐藏（阶段开放）" value="HIDDEN" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="clueForm.visibilityType === 'CHARACTER'" label="可见角色">
            <el-select v-model="clueForm.visibleCharacterId" clearable style="width: 100%">
              <el-option v-for="character in characters" :key="character.id"
                         :label="character.characterName" :value="character.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="可信度（1-10）">
            <el-input-number v-model="clueForm.credibility" :min="1" :max="10" style="width: 100%" />
          </el-form-item>
          <el-form-item label="事件时间"><el-input v-model="clueForm.eventTime" placeholder="2024-11-15 00:42" /></el-form-item>
          <el-form-item label="地点"><el-input v-model="clueForm.location" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="clueForm.sortOrder" :min="0" style="width: 100%" /></el-form-item>
          <el-form-item label="关键线索">
            <el-switch v-model="clueForm.isKeyClue" :active-value="1" :inactive-value="0" />
          </el-form-item>
          <el-form-item label="误导线索">
            <el-switch v-model="clueForm.isMisleading" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="clueDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveClue">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="relationDialog" title="新增证据关系" width="520px">
      <el-form :model="relationForm" label-position="top">
        <el-form-item label="线索">
          <el-select v-model="relationForm.clueId" filterable placeholder="选择线索" style="width: 100%">
            <el-option v-for="clue in clues" :key="clue.id" :label="clue.title" :value="clue.id" />
          </el-select>
        </el-form-item>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 14px">
          <el-form-item label="答案字段">
            <el-select v-model="relationForm.answerField" style="width: 100%">
              <el-option label="人物" value="CHARACTER" />
              <el-option label="时间" value="TIME" />
              <el-option label="地点" value="LOCATION" />
              <el-option label="行为" value="ACTION" />
              <el-option label="关键" value="KEY" />
            </el-select>
          </el-form-item>
          <el-form-item label="关系类型">
            <el-select v-model="relationForm.relationType" style="width: 100%">
              <el-option label="支持 SUPPORT" value="SUPPORT" />
              <el-option label="矛盾 CONTRADICT" value="CONTRADICT" />
              <el-option label="间接 INDIRECT" value="INDIRECT" />
              <el-option label="证据不足 INSUFFICIENT" value="INSUFFICIENT" />
              <el-option label="关键 KEY" value="KEY" />
              <el-option label="误导 MISLEADING" value="MISLEADING" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="分值">
          <el-input-number v-model="relationForm.score" :min="0" :max="50" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="relationDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRelation">保存</el-button>
      </template>
    </el-dialog>
  </div>
  <div v-else v-loading="true" class="panel" style="height: 240px"></div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api'

const DEFAULT_RULE = JSON.stringify({
  characterCorrect: 30,
  timeCorrect: 20,
  locationCorrect: 15,
  actionCorrect: 20,
  supportClueMax: 10,
  keyBonus: 10,
  contradictPerPenalty: 5,
  missingKeyPenalty: 5,
  timeConflictPenalty: 10
})

const props = defineProps({ id: { type: String, required: true } })
const router = useRouter()
const scriptId = computed(() => Number(props.id))

const script = ref(null)
const characters = ref([])
const stages = ref([])
const clues = ref([])
const relations = ref([])
const statistics = ref(null)
const tab = ref('basic')
const saving = ref(false)

const basic = reactive({
  title: '', description: '', story: '', difficulty: 'MEDIUM',
  minPlayers: 2, maxPlayers: 4, requiredPlayers: 4, estimatedMinutes: 60, coverImage: ''
})

const characterDialog = ref(false)
const characterForm = reactive({
  id: null, characterName: '', description: '', characterGoal: '', isHidden: 0, sortOrder: 0
})

const stageDialog = ref(false)
const stageForm = reactive({
  id: null, stageName: '', stageDescription: '', stageOrder: 1, unlockRule: '', timeoutMinutes: 60
})

const clueDialog = ref(false)
const clueForm = reactive({
  id: null, stageId: null, title: '', content: '', visibilityType: 'PUBLIC',
  visibleCharacterId: null, eventTime: '', location: '', credibility: 5,
  isKeyClue: 0, isMisleading: 0, sortOrder: 0
})

const relationDialog = ref(false)
const relationForm = reactive({
  clueId: null, answerField: 'CHARACTER', relationType: 'SUPPORT', score: 5
})

const answerForm = reactive({
  characterAnswer: '', timeAnswer: '', locationAnswer: '', actionAnswer: '', scoringRuleJson: DEFAULT_RULE
})

function statusText(status) {
  return { DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下架' }[status] || status
}

function visibilityText(value) {
  return { PUBLIC: '公开', CHARACTER: '指定角色', PLAYER: '指定玩家', HIDDEN: '阶段开放' }[value] || value
}

async function loadAll() {
  const res = await api.get(`/api/admin/scripts/${scriptId.value}`)
  script.value = res.data
  Object.assign(basic, {
    title: res.data.title,
    description: res.data.description,
    story: res.data.story,
    difficulty: res.data.difficulty,
    minPlayers: res.data.minPlayers,
    maxPlayers: res.data.maxPlayers,
    requiredPlayers: res.data.requiredPlayers,
    estimatedMinutes: res.data.estimatedMinutes,
    coverImage: res.data.coverImage
  })
  await Promise.all([loadCharacters(), loadStages(), loadClues(), loadAnswer(), loadRelations()])
}

async function loadCharacters() {
  const res = await api.get(`/api/admin/scripts/${scriptId.value}/characters`)
  characters.value = res.data || []
}

async function loadStages() {
  const res = await api.get(`/api/admin/scripts/${scriptId.value}/stages`)
  stages.value = res.data || []
}

async function loadClues() {
  const res = await api.get(`/api/admin/scripts/${scriptId.value}/clues`)
  clues.value = res.data || []
}

async function loadRelations() {
  const res = await api.get(`/api/admin/scripts/${scriptId.value}/relations`)
  relations.value = res.data || []
}

async function loadAnswer() {
  const res = await api.get(`/api/admin/scripts/${scriptId.value}/answer`)
  if (res.data) {
    Object.assign(answerForm, res.data)
  } else {
    Object.assign(answerForm, {
      characterAnswer: '', timeAnswer: '', locationAnswer: '', actionAnswer: '',
      scoringRuleJson: DEFAULT_RULE
    })
  }
}

async function onTabChange(name) {
  if (name === 'statistics') {
    try {
      const res = await api.get(`/api/admin/scripts/${scriptId.value}/statistics`)
      statistics.value = res.data
    } catch (e) {
      statistics.value = null
    }
  }
}

async function saveBasic() {
  saving.value = true
  try {
    await api.put(`/api/admin/scripts/${scriptId.value}`, { ...basic })
    ElMessage.success('基本信息已保存')
    await loadAll()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function publish() {
  try {
    await ElMessageBox.confirm('发布将校验角色数量、公开线索、关键线索和标准答案，确认发布？', '发布副本')
    await api.post(`/api/admin/scripts/${scriptId.value}/publish`)
    ElMessage.success('副本已发布')
    await loadAll()
  } catch (e) {
    // 取消或拦截器已提示
  }
}

async function offline() {
  try {
    await ElMessageBox.confirm('下架后新玩家无法加入该副本匹配，确认下架？', '下架副本')
    await api.post(`/api/admin/scripts/${scriptId.value}/offline`)
    ElMessage.success('副本已下架')
    await loadAll()
  } catch (e) {
    // 取消或拦截器已提示
  }
}

function openCharacter(row) {
  Object.assign(characterForm, row
    ? {
        id: row.id, characterName: row.characterName, description: row.description,
        characterGoal: row.characterGoal, isHidden: row.isHidden, sortOrder: row.sortOrder
      }
    : { id: null, characterName: '', description: '', characterGoal: '', isHidden: 0, sortOrder: 0 })
  characterDialog.value = true
}

async function saveCharacter() {
  if (!characterForm.characterName) {
    ElMessage.warning('请填写角色名称')
    return
  }
  saving.value = true
  try {
    const base = `/api/admin/scripts/${scriptId.value}/characters`
    const payload = {
      characterName: characterForm.characterName,
      description: characterForm.description,
      characterGoal: characterForm.characterGoal,
      isHidden: characterForm.isHidden,
      sortOrder: characterForm.sortOrder
    }
    if (characterForm.id) {
      await api.put(`${base}/${characterForm.id}`, payload)
    } else {
      await api.post(base, payload)
    }
    ElMessage.success('角色已保存')
    characterDialog.value = false
    await loadCharacters()
    await loadClues()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function removeCharacter(row) {
  try {
    await ElMessageBox.confirm(`确认删除角色「${row.characterName}」？`, '删除角色', { type: 'warning' })
    await api.delete(`/api/admin/scripts/${scriptId.value}/characters/${row.id}`)
    ElMessage.success('角色已删除')
    await loadCharacters()
    await loadClues()
  } catch (e) {
    // 取消或拦截器已提示
  }
}

function openStage(row) {
  Object.assign(stageForm, row
    ? {
        id: row.id, stageName: row.stageName, stageDescription: row.stageDescription,
        stageOrder: row.stageOrder, unlockRule: row.unlockRule, timeoutMinutes: row.timeoutMinutes
      }
    : { id: null, stageName: '', stageDescription: '', stageOrder: stages.value.length + 1, unlockRule: '', timeoutMinutes: 60 })
  stageDialog.value = true
}

async function saveStage() {
  if (!stageForm.stageName) {
    ElMessage.warning('请填写阶段名称')
    return
  }
  saving.value = true
  try {
    const base = `/api/admin/scripts/${scriptId.value}/stages`
    const payload = {
      stageName: stageForm.stageName,
      stageDescription: stageForm.stageDescription,
      stageOrder: stageForm.stageOrder,
      unlockRule: stageForm.unlockRule,
      timeoutMinutes: stageForm.timeoutMinutes
    }
    if (stageForm.id) {
      await api.put(`${base}/${stageForm.id}`, payload)
    } else {
      await api.post(base, payload)
    }
    ElMessage.success('阶段已保存')
    stageDialog.value = false
    await loadStages()
    await loadClues()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function removeStage(row) {
  try {
    await ElMessageBox.confirm(`确认删除阶段「${row.stageName}」？`, '删除阶段', { type: 'warning' })
    await api.delete(`/api/admin/scripts/${scriptId.value}/stages/${row.id}`)
    ElMessage.success('阶段已删除')
    await loadStages()
    await loadClues()
  } catch (e) {
    // 取消或拦截器已提示
  }
}

function openClue(row) {
  Object.assign(clueForm, row
    ? {
        id: row.id, stageId: row.stageId, title: row.title, content: row.content,
        visibilityType: row.visibilityType, visibleCharacterId: row.visibleCharacterId,
        eventTime: row.eventTime, location: row.location, credibility: row.credibility,
        isKeyClue: row.isKeyClue, isMisleading: row.isMisleading, sortOrder: row.sortOrder
      }
    : {
        id: null, stageId: null, title: '', content: '', visibilityType: 'PUBLIC',
        visibleCharacterId: null, eventTime: '', location: '', credibility: 5,
        isKeyClue: 0, isMisleading: 0, sortOrder: clues.value.length + 1
      })
  clueDialog.value = true
}

async function saveClue() {
  if (!clueForm.title || !clueForm.content) {
    ElMessage.warning('请填写线索标题和内容')
    return
  }
  saving.value = true
  try {
    const base = `/api/admin/scripts/${scriptId.value}/clues`
    const payload = {
      stageId: clueForm.stageId || null,
      title: clueForm.title,
      content: clueForm.content,
      visibilityType: clueForm.visibilityType,
      visibleCharacterId: clueForm.visibilityType === 'CHARACTER' ? clueForm.visibleCharacterId : null,
      eventTime: clueForm.eventTime,
      location: clueForm.location,
      credibility: clueForm.credibility,
      isKeyClue: clueForm.isKeyClue,
      isMisleading: clueForm.isMisleading,
      sortOrder: clueForm.sortOrder
    }
    if (clueForm.id) {
      await api.put(`${base}/${clueForm.id}`, payload)
    } else {
      await api.post(base, payload)
    }
    ElMessage.success('线索已保存')
    clueDialog.value = false
    await loadClues()
    await loadRelations()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function removeClue(row) {
  try {
    await ElMessageBox.confirm(`确认删除线索「${row.title}」？相关证据关系也会被删除。`, '删除线索', { type: 'warning' })
    await api.delete(`/api/admin/scripts/${scriptId.value}/clues/${row.id}`)
    ElMessage.success('线索已删除')
    await loadClues()
    await loadRelations()
  } catch (e) {
    // 取消或拦截器已提示
  }
}

async function saveAnswer() {
  saving.value = true
  try {
    await api.put(`/api/admin/scripts/${scriptId.value}/answer`, { ...answerForm })
    ElMessage.success('标准答案与评分规则已保存')
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

function openRelation() {
  relationForm.clueId = null
  relationForm.answerField = 'CHARACTER'
  relationForm.relationType = 'SUPPORT'
  relationForm.score = 5
  relationDialog.value = true
}

async function saveRelation() {
  if (!relationForm.clueId) {
    ElMessage.warning('请选择线索')
    return
  }
  saving.value = true
  try {
    await api.post(`/api/admin/scripts/${scriptId.value}/relations`, { ...relationForm })
    ElMessage.success('证据关系已添加')
    relationDialog.value = false
    await loadRelations()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function removeRelation(row) {
  try {
    await ElMessageBox.confirm('确认删除该证据关系？', '删除关系', { type: 'warning' })
    await api.delete(`/api/admin/scripts/${scriptId.value}/relations/${row.id}`)
    ElMessage.success('证据关系已删除')
    await loadRelations()
  } catch (e) {
    // 取消或拦截器已提示
  }
}

onMounted(loadAll)
</script>
