import { createRouter, createWebHistory } from 'vue-router'
import { ensureUser } from '../stores/auth'
import MainLayout from '../layouts/MainLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import LobbyView from '../views/LobbyView.vue'
import ScriptDetailView from '../views/ScriptDetailView.vue'
import MatchmakingView from '../views/MatchmakingView.vue'
import RoomView from '../views/RoomView.vue'
import ProfileView from '../views/ProfileView.vue'
import PasswordView from '../views/PasswordView.vue'
import HistoryView from '../views/HistoryView.vue'
import AdminScriptsView from '../views/admin/AdminScriptsView.vue'
import AdminScriptEditView from '../views/admin/AdminScriptEditView.vue'
import AdminUsersView from '../views/admin/AdminUsersView.vue'
import AdminRoomsView from '../views/admin/AdminRoomsView.vue'

const routes = [
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/register', component: RegisterView, meta: { public: true } },
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', redirect: '/lobby' },
      { path: 'lobby', component: LobbyView },
      { path: 'scripts/:id', component: ScriptDetailView, props: true },
      { path: 'match/:scriptId', component: MatchmakingView, props: true },
      { path: 'rooms/:id', component: RoomView, props: true },
      { path: 'profile', component: ProfileView },
      { path: 'password', component: PasswordView },
      { path: 'history', component: HistoryView }
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { admin: true },
    children: [
      { path: '', redirect: '/admin/scripts' },
      { path: 'scripts', component: AdminScriptsView },
      { path: 'scripts/:id', component: AdminScriptEditView, props: true },
      { path: 'users', component: AdminUsersView },
      { path: 'rooms', component: AdminRoomsView }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/lobby' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  if (to.meta.public) {
    return true
  }
  const user = await ensureUser()
  if (!user) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.matched.some((record) => record.meta.admin) && user.role !== 'ADMIN') {
    return '/lobby'
  }
  return true
})

export default router
