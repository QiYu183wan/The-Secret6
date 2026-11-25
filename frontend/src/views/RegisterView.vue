<template>
  <div class="auth-wrap">
    <div class="auth-hero">
      <h1>入局</h1>
      <p>
        注册后系统会自动为你登录。<br />
        你将进入副本大厅，选择剧本并等待其他玩家加入。<br />
        每个副本都会随机分配角色，<br />
        并只向你展示该角色可见的线索。
      </p>
      <div class="meta-row" style="margin-top: 30px">
        <span class="chip red">客观评分</span>
        <span class="chip red">阶段解锁</span>
        <span class="chip red">证据链复盘</span>
      </div>
      <div style="margin-top: 40px; padding: 20px; border-left: 3px solid #c41e3a; font-style: italic; color: #8a7a95; font-size: 13px; letter-spacing: 2px; line-height: 2">
        "收集线索、交换证词、构建证据链——这就是谜局的解谜之道。"
      </div>
    </div>

    <div class="auth-panel">
      <h2>创建账号</h2>
      <p class="sub">密码将使用 BCrypt 加密后保存到数据库。</p>
      <el-form :model="form" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="3-32 位用户名" size="large" clearable />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="其他玩家看到的名字" size="large" clearable />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="6-32 位密码" size="large" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.confirm" type="password" placeholder="再次输入密码" size="large" show-password
                    @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
          注册并登录
        </el-button>
      </el-form>
      <div style="margin-top: 20px; display: flex; justify-content: space-between; font-size: 13px">
        <span class="muted">已有账号？</span>
        <router-link to="/login" class="red" style="font-weight: 700; letter-spacing: 2px">返回登录 →</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'
import { setUser } from '../stores/auth'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', nickname: '', password: '', confirm: '' })

async function submit() {
  if (!form.username || !form.nickname || !form.password) {
    ElMessage.warning('请完整填写注册信息')
    return
  }
  if (form.password !== form.confirm) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    const res = await api.post('/api/register', {
      username: form.username,
      nickname: form.nickname,
      password: form.password
    })
    setUser(res.data)
    ElMessage.success('注册成功，已自动登录')
    router.push('/lobby')
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}
</script>
