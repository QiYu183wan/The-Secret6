# 谜局：证据链驱动的多人推理平台

基于 **Spring Boot 3 + Vue 3** 的前后端分离多人推理平台。副本、角色、阶段、线索、证据关系、标准答案与评分规则全部由数据库驱动，管理员可在后台新增副本，无需修改代码。推理结算由后端按预设规则客观计算，不使用 AI 或随机答案。

## 一、技术栈

| 层次 | 技术 |
| --- | --- |
| 后端 | JDK 17、Spring Boot 3.2.5、Spring Security（Session 登录）、Spring Validation |
| 数据访问 | MyBatis 3.5（mybatis-spring-boot-starter）+ Mapper XML |
| 数据库 | MySQL 8 |
| 缓存 | Redis（副本列表 / 详情缓存，不可用时自动降级为数据库读取） |
| 前端 | Vue 3、Vite 5、Vue Router 4、Axios、Element Plus |
| 构建 | Maven、npm |

## 二、目录结构

```
The Secret4
├── backend                     Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/enigma
│       ├── config              安全配置
│       ├── controller          玩家端与管理端接口
│       ├── dto                 请求 / 视图对象
│       ├── entity              15 张表的实体
│       ├── mapper              MyBatis Mapper 接口（SQL 位于 resources/mapper/*.xml）
│       ├── security            登录用户与权限
│       └── service             副本、匹配、房间、评分、缓存
├── frontend                    Vue 3 前端
│   └── src
│       ├── api                 Axios 封装
│       ├── layouts             玩家端 / 管理端布局
│       ├── router              路由与权限守卫
│       ├── stores              登录态
│       ├── views               玩家端页面
│       └── views/admin         管理后台页面
├── schema.sql                  数据库建表脚本
├── data.sql                    《午夜档案》演示副本数据
└── 迷局简化版描述.txt           需求说明
```

## 三、运行前准备

1. **JDK 17**（IDEA 中将 Project SDK 与 Maven Runner JRE 设为 17）
2. **MySQL 8**，已启动
3. **Redis**（可选，未启动时副本缓存自动降级为 MySQL）
4. **Node.js 18+** 与 npm
5. IDEA 打开项目根目录 `The Secret4`，会自动识别 `backend/pom.xml` 与 `frontend/package.json`

### 数据库初始化

默认连接配置在 `backend/src/main/resources/application.yml`：

```yaml
url: jdbc:mysql://localhost:3306/enigma_db?...
username: root
password: 123456
```

如本机密码不同，请修改该文件。然后执行：

```bash
mysql -uroot -p123456 < schema.sql
mysql -uroot -p123456 < data.sql
```

脚本会创建 `enigma_db`、全部 15 张表，并写入《午夜档案》演示副本（4 个角色、3 个阶段、11 条线索、标准答案与证据关系）。

演示账号由后端首次启动时自动写入（密码使用 BCrypt 加密）：

| 账号 | 密码 | 身份 |
| --- | --- | --- |
| admin | admin123 | 管理员 |
| player1 ~ player4 | 123456 | 普通玩家 |

## 四、启动项目

### 1. 启动后端

方式 A（IDEA）：打开 `backend/src/main/java/com/enigma/EnigmaApplication.java`，直接运行。

方式 B（命令行）：

```bash
cd backend
mvn spring-boot:run
```

后端地址：`http://localhost:8082`

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`。Vite 已将 `/api` 代理到 `http://localhost:8082`，Cookie Session 可正常携带。

### 3. 快速体验一局

1. 打开 4 个浏览器（或无痕窗口），分别登录 `player1` ~ `player4`
2. 进入「副本大厅 → 午夜档案 → 开始匹配」
3. 4 人都加入后自动创建房间并随机分配角色
4. 各玩家查看自己的线索，填写人物 / 时间 / 地点 / 行为，勾选引用线索后提交
5. 全员提交后自动结算，可查看得分明细与案件复盘

管理员 `admin / admin123` 登录后可进入「管理后台」，新增或编辑副本、角色、阶段、线索、标准答案与证据关系。

## 五、功能概览

- 账号：注册自动登录、登录 / 退出、资料修改、密码修改、管理员禁用账号
- 权限：Spring Security Session 登录，`PLAYER` / `ADMIN` 角色隔离，标准答案接口仅后台可用
- 副本：草稿 / 发布 / 下架，发布前校验开局人数、角色数量、公开线索、关键线索、标准答案
- 匹配：按副本排队，达到开局人数后事务内建房、随机分配角色、写入房间玩家
- 线索：按 `PUBLIC / CHARACTER / HIDDEN` 与阶段顺序由后端过滤，玩家无法读取他人私人线索
- 阶段：初始阶段自动开放，提交推理解锁第二阶段，结算后全部开放
- 推理：结构化提交人物、时间、地点、行为与引用线索
- 评分：人物 30 / 时间 20 / 地点 15 / 行为 20，支持线索最多 +10，关键线索 +10，矛盾 / 误导 / 缺失关键线索 / 时间冲突扣分，总分限制 0–100
- 结算：全员提交自动结算，生成得分明细与游戏结果，支持历史记录与完整复盘
- 缓存：Redis 缓存已发布副本列表与详情，管理员修改后清理缓存；Redis 异常时自动回源 MySQL

## 六、主要接口

| 模块 | 接口 |
| --- | --- |
| 账号 | `POST /api/register`、`POST /api/login`、`POST /api/logout` |
| 个人 | `GET/PUT /api/user/profile`、`PUT /api/user/password`、`GET /api/user/history` |
| 副本 | `GET /api/scripts`、`GET /api/scripts/{id}` |
| 匹配 | `POST /api/matchmaking/{scriptId}/join`、`GET /api/matchmaking/{scriptId}/status`、`DELETE /api/matchmaking/{scriptId}/cancel` |
| 房间 | `GET /api/rooms/{roomId}`、`/character`、`/stage`、`/clues`、`/evidence-chain`、`/result`、`/replay` |
| 提交 | `POST /api/rooms/{roomId}/submission` |
| 后台副本 | `GET/POST /api/admin/scripts`、`PUT /api/admin/scripts/{id}`、`POST .../publish`、`POST .../offline` |
| 后台内容 | `/api/admin/scripts/{id}/characters`、`/stages`、`/clues`、`/answer`、`/relations`、`/statistics` |
| 后台运营 | `GET /api/admin/users`、`PUT /api/admin/users/{id}/status`、`GET /api/admin/rooms` |

统一返回结构：`{ "code": 200, "message": "success", "data": ... }`。未登录返回 401，无权限返回 403。

## 七、验证要点

1. 注册后账号写入 `sys_user`，密码为 BCrypt 密文，并自动建立登录态
2. 修改密码后旧密码失效、新密码可登录
3. 普通玩家访问 `/api/admin/**` 返回 403
4. 管理员可新增、编辑、发布、下架副本，并配置角色 / 阶段 / 线索 / 答案 / 关系
5. 发布校验：开局人数与角色数量一致、至少一条公开线索、至少一条关键线索、存在标准答案
6. 4 名玩家匹配后只创建一个房间，角色随机且写入 `room_player`
7. 不同角色只能看到公开线索与自己角色的私人线索
8. 提交后第二阶段线索开放，全员提交后自动结算
9. 相同副本、相同答案、相同线索引用得到相同分数
10. 结算结果写入 `game_result`，刷新页面仍可回看
11. 关闭 Redis 后副本列表仍可从 MySQL 读取；管理员修改副本后缓存被清理

## 八、常见问题

- **启动报数据库连接失败**：检查 MySQL 是否启动、`application.yml` 中的账号密码、是否已执行 `schema.sql` 与 `data.sql`。
- **端口被占用**：先停止其他 Spring Boot 进程（例如之前运行过的旧项目），或修改 `backend/src/main/resources/application.yml` 的 `server.port` 与 `frontend/vite.config.js` 的代理地址，两者需保持一致。
- **Redis 连接失败日志**：不影响使用，系统已做降级处理；启动 Redis 后缓存会自动恢复。
- **前端 401**：Session 过期，重新登录即可。
- **匹配一直等待**：演示副本需要 4 个账号同时加入，可用 4 个浏览器窗口分别登录 `player1` ~ `player4`。
- **前端依赖安装慢**：可先执行 `npm config set registry https://registry.npmmirror.com` 再 `npm install`。
