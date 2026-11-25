<template>
  <div>
    <div class="page-head">
      <div>
        <h1 class="page-title">个人中心</h1>
        <p class="page-sub">账号信息保存在 MySQL，密码使用 BCrypt 加密</p>
      </div>
    </div>

    <div class="detail-grid">
      <div class="panel">
        <h3 class="panel-title">编辑资料</h3>
        <el-form :model="form" label-position="top">
          <el-form-item label="用户名">
            <el-input :model-value="user?.username" disabled />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" maxlength="32" show-word-limit />
          </el-form-item>
          <el-form-item label="头像地址">
            <el-input v-model="form.avatar" placeholder="https://..." />
          </el-form-item>
          <el-form-item label="个人简介">
            <el-input v-model="form.bio" type="textarea" :rows="4" maxlength="512" show-word-limit />
          </el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存资料</el-button>
          <el-button @click="router.push('/password')">修改密码</el-button>
        </el-form>
      </div>

      <div>
        <div class="panel" style="text-align: center">
          <el-avatar :size="90" :src="user?.avatar || undefined" style="margin-bottom: 16px; border: 2px solid #c41e3a; box-shadow: 0 0 20px rgba(196, 30, 58, 0.3)">
            {{ (user?.nickname || '?').slice(0, 1) }}
          </el-avatar>
          <h3 style="margin: 0 0 8px; font-family: 'Ma Shan Zheng', serif; letter-spacing: 3px; color: #e8e0d5">{{ user?.nickname }}</h3>
          <p class="muted" style="margin: 0 0 18px">{{ user?.username }}</p>
          <div class="meta-row" style="justify-content: center">
            <span class="chip gold" v-if="user?.role === 'ADMIN'">👑 管理员</span>
            <span class="chip" v-else>🎭 普通玩家</span>
            <span class="chip" :class="{ 'red': user?.status !== 'ACTIVE' }">{{ user?.status === 'ACTIVE' ? '账号正常' : '账号禁用' }}</span>
          </div>
        </div>
        <div class="panel">
          <h3 class="panel-title">快捷入口</h3>
          <div style="display: flex; flex-direction: column; gap: 12px">
            <el-button @click="router.push('/lobby')">🏛 副本大厅</el-button>
            <el-button @click="router.push('/history')">📜 推理记录</el-button>
            <el-button v-if="user?.role === 'ADMIN'" type="primary" plain @click="router.push('/admin/scripts')">
              🎭 进入管理后台
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'
import { auth, setUser } from '../stores/auth'

const router = useRouter()
const user = computed(() => auth.user)
const saving = ref(false)
const form = reactive({ nickname: '', avatar: '', bio: '' })

function fillForm(data) {
  form.nickname = data?.nickname || ''
  form.avatar = data?.avatar || ''
  form.bio = data?.bio || ''
}

async function save() {
  if (!form.nickname) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    const res = await api.put('/api/user/profile', { ...form })
    setUser(res.data)
    fillForm(res.data)
    ElMessage.success('资料已更新')
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    const res = await api.get('/api/user/profile')
    setUser(res.data)
    fillForm(res.data)
  } catch (e) {
    fillForm(auth.user)
  }
})
</script>
