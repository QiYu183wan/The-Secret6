<template>
  <div style="max-width: 560px; margin: 0 auto">
    <div class="page-head">
      <div>
        <h1 class="page-title">修改密码</h1>
        <p class="page-sub">修改成功后旧密码立即失效，新密码可用于下次登录</p>
      </div>
    </div>

    <div class="panel">
      <el-form :model="form" label-position="top">
        <el-form-item label="当前密码">
          <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="6-32 位新密码" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="form.confirm" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
        <el-button type="primary" :loading="saving" @click="submit">确认修改</el-button>
        <el-button @click="router.push('/profile')">← 返回个人中心</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const saving = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirm: '' })

async function submit() {
  if (!form.oldPassword || !form.newPassword) {
    ElMessage.warning('请填写当前密码和新密码')
    return
  }
  if (form.newPassword !== form.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  saving.value = true
  try {
    await api.put('/api/user/password', {
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    ElMessage.success('密码修改成功')
    form.oldPassword = ''
    form.newPassword = ''
    form.confirm = ''
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}
</script>
