# 智能简历诊断系统 (IRAS)

> Intelligent Resume Analysis System — 基于 B/S 架构的招聘辅助平台

## 系统架构

```
┌─────────────────────────────────────────────────┐
│                   前端 (Vue 3)                   │
│  Element Plus + Vue Router + Pinia + Axios + ECharts │
│         http://localhost:5173                     │
├──────────────────────┬──────────────────────────┤
│                      │                           │
│     Vite Proxy       │      /iras/api/*          │
│                      ▼                           │
│              ┌──────────────┐                    │
│              │  后端 (Spring Boot)  │             │
│              │  Spring Security    │             │
│              │  JWT + MyBatis      │             │
│              │  http://localhost:8080/iras        │
│              └───────┬──────────┘                │
│                      │                           │
│          ┌───────────┼───────────┐               │
│          ▼           ▼           ▼               │
│      ┌──────┐  ┌──────────┐  ┌──────────┐       │
│      │ MySQL │  │ Dify API │  │ Dify API │       │
│      │ iras  │  │ 职能画像  │  │ 简历诊断  │       │
│      └──────┘  └──────────┘  └──────────┘       │
└─────────────────────────────────────────────────┘
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Element Plus + Vue Router + Pinia + Axios + ECharts |
| 后端 | Spring Boot 3.2 + Spring Security + JWT + MyBatis |
| 数据库 | MySQL 8.0 |
| AI 引擎 | Dify Workflow API（SSE 流式调用） |

## 功能模块

### 用户端

| 功能 | 说明 |
|------|------|
| **用户认证** | 用户名/邮箱登录注册，JWT 无状态会话，Token 有效期 24 小时 |
| **职位检索** | 按职位名称、城市、薪资范围多字段组合筛选，分页展示，支持查看详情 |
| **职能画像** | 输入职位名称，AI 自动生成能力画像（硬技能、软技能、工具、经验、学历） |
| **智能诊断** | 上传简历文件（.txt/.pdf/.doc/.docx，最大 10MB），AI 分析匹配岗位，生成诊断报告 |
| **诊断历史** | 查看、回顾、删除过往诊断记录，支持分页和详情查看 |

### 管理端（admin 角色）

| 功能 | 说明 |
|------|------|
| **系统概览** | ECharts 可视化仪表盘：用户/职位/诊断统计卡片、7 天注册趋势折线图、7 天诊断趋势折线图、城市分布饼图 |
| **用户管理** | 查看用户列表、修改用户角色（user/admin）、删除用户 |
| **职位管理** | 新增、编辑、删除职位信息 |

## 快速启动

### 1. 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### 2. 数据库初始化

```bash
mysql -u root -p < backend/src/main/resources/init.sql
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端运行在 http://localhost:8080/iras

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 http://localhost:5173

### 5. 创建管理员

注册第一个用户后，在数据库中将其设为管理员：

```sql
USE iras;
UPDATE user SET role = 'admin' WHERE username = '你的用户名';
```

重新登录后即可看到管理后台菜单。

## 项目结构

```
iras-project/
├── backend/                          # Spring Boot 后端
│   ├── pom.xml                       # Maven 配置
│   └── src/main/
│       ├── java/com/iras/
│       │   ├── IrasApplication.java  # 启动类
│       │   ├── config/               # 安全配置、JWT 过滤器
│       │   ├── controller/           # REST 控制器
│       │   │   ├── AuthController    # 认证（注册/登录）
│       │   │   ├── JobController     # 职位（搜索/详情）
│       │   │   ├── DifyController    # AI（职能画像/简历诊断）
│       │   │   ├── DiagnosisController # 诊断历史
│       │   │   └── AdminController   # 管理后台
│       │   ├── dto/                  # 数据传输对象
│       │   ├── entity/               # 实体类
│       │   ├── mapper/               # MyBatis Mapper
│       │   ├── service/              # 业务逻辑
│       │   └── util/                 # JWT 工具类
│       └── resources/
│           ├── application.yml       # 配置文件
│           ├── init.sql              # 数据库初始化
│           └── mapper/               # MyBatis XML
│
└── frontend/                         # Vue 3 前端
    ├── package.json
    ├── vite.config.js                # Vite 配置（含代理）
    └── src/
        ├── api/                      # Axios 封装
        ├── router/                   # 路由配置
        ├── store/                    # Pinia 状态管理
        └── views/                    # 页面组件
            ├── Login.vue             # 登录/注册
            ├── Layout.vue            # 主布局（侧边栏 + keep-alive）
            ├── Jobs.vue              # 职位检索
            ├── JobProfile.vue        # 职能画像
            ├── Diagnosis.vue         # 智能诊断
            ├── History.vue           # 诊断历史
            ├── AdminDashboard.vue    # 管理员仪表盘（ECharts）
            ├── AdminUsers.vue        # 用户管理
            └── AdminJobs.vue         # 职位管理
```

## API 接口

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /iras/api/auth/register | 注册 |
| POST | /iras/api/auth/login | 登录 |

### 职位

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /iras/api/jobs/search | 搜索（参数：jobName, city, salaryMin, salaryMax, page, size） |
| GET | /iras/api/jobs/{id} | 详情 |

### AI

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /iras/api/dify/job-profile | 职能画像（JSON body: { "job_name": "..." }） |
| POST | /iras/api/dify/diagnose | 简历诊断（multipart/form-data，字段名: file） |

### 诊断历史

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /iras/api/diagnosis/history | 历史列表（分页：page, size） |
| GET | /iras/api/diagnosis/detail/{id} | 记录详情 |
| DELETE | /iras/api/diagnosis/{id} | 删除记录 |

### 管理后台（需 admin 角色）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /iras/api/admin/users | 用户列表 |
| PUT | /iras/api/admin/users/{id}/role | 修改角色 |
| DELETE | /iras/api/admin/users/{id} | 删除用户 |
| GET | /iras/api/admin/jobs | 职位列表 |
| POST | /iras/api/admin/jobs | 新增职位 |
| PUT | /iras/api/admin/jobs/{id} | 更新职位 |
| DELETE | /iras/api/admin/jobs/{id} | 删除职位 |
| GET | /iras/api/admin/statistics | 系统统计 |

## 配置说明

Dify API 配置在 `backend/src/main/resources/application.yml` 中：

```yaml
dify:
  base-url: https://api.dify.ai/v1
  job-profile-key: <your-job-profile-app-key>
  resume-diagnosis-key: <your-resume-diagnosis-app-key>
```

## 注意事项

- Dify AI 推理时间约为 2-3 分钟，前端已设置 5 分钟超时
- Dify API 使用 SSE 流式调用（response_mode: streaming），内置 502/503/504 自动重试
- 页面使用 `keep-alive` 缓存，通过 `onActivated` 钩子确保切换路由时数据自动刷新
- 诊断记录中推理过程（think 标签）与最终结果分离存储，前端正确分区展示
- 管理员仪表盘使用 ECharts 渲染趋势折线图和城市分布饼图
- JWT Token 有效期为 24 小时
- 简历诊断的 Dify workflow 要求 `resume_text` 输入变量为文件列表类型（ArrayFiles），后端已做适配
