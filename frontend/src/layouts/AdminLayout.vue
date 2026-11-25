<template>
  <div>
    <header class="topbar">
      <div class="brand" @click="go('/admin/scripts')">
        <div class="brand-mark">局</div>
        <div>
          <div class="brand-title">谜局后台</div>
          <div class="brand-sub">MASTERMIND CONSOLE</div>
        </div>
      </div>
      <nav class="nav">
        <router-link class="nav-link" to="/admin/scripts">副本管理</router-link>
        <router-link class="nav-link" to="/admin/users">用户账号</router-link>
        <router-link class="nav-link" to="/admin/rooms">房间记录</router-link>
        <router-link class="nav-link" to="/lobby">返回玩家端</router-link>
      </nav>
      <div class="topbar-right">
        <el-dropdown @command="handleCommand">
          <span class="user-chip">
            <el-avatar :size="28" :src="auth.user?.avatar || undefined">{{ initial }}</el-avatar>
            <span>{{ auth.user?.nickname || auth.user?.username }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人资料</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    <main class="page">
      <router-view />
    </main>
    <footer class="footer">副本与证据管理后台 · 数据由 MySQL 驱动</footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { auth, logout } from '../stores/auth'

const router = useRouter()
const initial = computed(() => (auth.user?.nickname || auth.user?.username || '管').slice(0, 1))

function go(path) {
  router.push(path)
}

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    await logout()
    router.push('/login')
  }
}
</script>
