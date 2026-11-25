import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import App from './App.vue'
import router from './router'
import './styles.css'

document.documentElement.classList.add('dark')

const app = createApp(App)
  .use(router)
  .use(ElementPlus, {
    locale: zhCn,
    // 自定义主题色 - 弹丸论破血红
    size: 'default',
    zIndex: 3000
  })

app.mount('#app')
