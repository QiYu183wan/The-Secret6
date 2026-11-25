<template>
  <div class="auth-wrap">
    <div class="auth-hero">
      <h1>谜局</h1>
      <p>
        午夜时分，一封档案在闭馆后悄然失踪。<br />
        每个人都只掌握部分真相，<br />
        收集线索、交换证词、构建证据链——<br />
        用可解释的推理，还原案件的全貌。
      </p>
      <div class="meta-row" style="margin-top: 30px">
        <span class="chip red">☠ Spring Boot</span>
        <span class="chip red">☠ Vue 3</span>
        <span class="chip">MySQL</span>
        <span class="chip">Redis</span>
        <span class="chip">Spring Security</span>
      </div>
      <div style="margin-top: 40px; padding: 20px; border-left: 3px solid #c41e3a; font-style: italic; color: #8a7a95; font-size: 13px; letter-spacing: 2px; line-height: 2">
        "真相只有一个——但它需要被所有人看见。"
      </div>
    </div>

    <div class="auth-panel">
      <h2>登录谜局</h2>
      <p class="sub">用账号进入副本大厅，开启你的解谜之旅。</p>
      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" clearable />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large"
                    show-password @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
          开始解谜
        </el-button>
      </el-form>
      <div style="margin-top: 20px; display: flex; justify-content: space-between; font-size: 13px">
        <span class="muted">还没有账号？</span>
        <router-link to="/register" class="red" style="font-weight: 700; letter-spacing: 2px">注册新账号 →</router-link>
      </div>
      <el-divider style="margin: 24px 0 18px" />
      <div class="muted" style="font-size: 12px; line-height: 2; letter-spacing: 1px">
        <span class="gold">演示账号</span>：admin / admin123（管理员）<br />
        <span class="gold">演示玩家</span>：player1 ~ player4 / 123456
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'
import { setUser } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await api.post('/api/login', form)
    setUser(res.data)
    ElMessage.success('欢迎回来，侦探')
    router.push(route.query.redirect || '/lobby')
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>
