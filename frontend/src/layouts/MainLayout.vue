<template>
  <div>
    <header class="topbar">
      <div class="brand" @click="go('/lobby')">
        <div class="brand-mark">谜</div>
        <div>
          <div class="brand-title">谜局</div>
          <div class="brand-sub">TRIAL OF THE BLACK &amp; WHITE</div>
        </div>
      </div>
      <nav class="nav">
        <router-link class="nav-link" to="/lobby">副本大厅</router-link>
        <router-link class="nav-link" to="/history">推理记录</router-link>
        <router-link class="nav-link" to="/profile">个人中心</router-link>
      </nav>
      <div class="topbar-right">
        <el-button v-if="isAdmin" text class="gold" @click="go('/admin/scripts')">管理后台</el-button>
        <el-dropdown @command="handleCommand">
          <span class="user-chip">
            <el-avatar :size="28" :src="auth.user?.avatar || undefined">{{ initial }}</el-avatar>
            <span>{{ auth.user?.nickname || auth.user?.username || '玩家' }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人资料</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    <main class="page">
      <router-view />
    </main>
    <footer class="footer">谜局 · 审判之夜 · Spring Boot + Vue 3</footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { auth, logout } from '../stores/auth'

const router = useRouter()
const isAdmin = computed(() => auth.user?.role === 'ADMIN')
const initial = computed(() => (auth.user?.nickname || auth.user?.username || '?').slice(0, 1))

function go(path) {
  router.push(path)
}

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'password') {
    router.push('/password')
  } else if (command === 'logout') {
    await logout()
    router.push('/login')
  }
}
</script>
