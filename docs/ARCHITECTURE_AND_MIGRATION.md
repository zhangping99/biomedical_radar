# 医药雷达跨版本架构与迁移规范

版本：1.0  
状态：开发基线  
适用范围：V1.0、V2.0、V3.0

## 1. 架构目标

项目采用“一套领域模型、同一套前端、逐阶段增强后端”的路线：

```text
V1：Vue 3 PWA + 静态JSON + GitHub Actions + GitHub Pages
                         ↓ 兼容迁移
V2：Vue 3 PWA + Spring Boot + MySQL + Docker + Tailscale
                         ↓ 兼容迁移
V3：Vue 3 PWA + Spring Boot + MySQL + 公共云 + 境外采集节点
```

V1不是一次性原型。Article契约、来源适配器、分类体系和前端领域对象必须可以在V2/V3继续使用。

## 2. 核心边界

### 2.1 前端边界

前端只读取统一的Repository接口：

```text
ArticleRepository
  - StaticArticleRepository（V1，读取JSON）
  - ApiArticleRepository（V2/V3，读取/api/v1）
```

页面和Pinia Store不得直接拼接GitHub Pages路径或Spring Boot接口。数据源由构建环境变量选择：

```text
VITE_DATA_MODE=static | api
```

### 2.2 后端边界

后端分为：

- `radar-domain`：Article、Source、分类、去重规则和端口接口；
- `radar-collector`：RSS、REST、HTML公告连接器、规范化和静态导出；
- `radar-api`：V2起启用的REST、鉴权、JPA和管理功能。

`radar-domain`不得依赖Spring MVC、JPA实体或具体来源SDK。

### 2.3 来源边界

每个来源必须实现统一生命周期：

```text
discover → fetch → parse → normalize → validate → deduplicate → enrich → publish
```

来源失败只影响该来源，不得使整个批次失败。每次执行输出来源级状态报告。

## 3. Article稳定契约

以下字段从V1开始保留，后续版本只允许兼容新增：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| id | string | 是 | 稳定UUID/ULID，不因标题翻译改变 |
| sourceId | string | 是 | 来源配置ID |
| sourceName | string | 是 | 用户可见来源名 |
| sourceTier | A/B/C | 是 | A监管/注册库/论文索引/正式期刊等一手来源，B机构自述或未同行评议来源，C仅作待核验线索 |
| sourceType | enum | 是 | regulator、registry、journal、company、exchange、media |
| originalUrl | string | 是 | 原始链接 |
| canonicalUrl | string | 是 | 去跟踪参数后的规范URL |
| titleOriginal | string | 是 | 原始标题 |
| titleZh | string/null | 否 | 中文标题 |
| summaryZh | string/null | 否 | 中文事实摘要 |
| whyItMattersZh | string/null | 否 | 关注理由，不得提供医疗/投资建议 |
| language | string | 是 | BCP 47语言代码 |
| region | enum | 是 | CN、US、EU、JP、GLOBAL、OTHER |
| category | enum | 是 | 一级栏目 |
| eventTypes | string[] | 是 | 获批、临床结果、融资等 |
| entities | object[] | 是 | 企业、药品、医院、机构等 |
| diseaseAreas | string[] | 是 | 疾病领域 |
| publishedAt | datetime/null | 否 | 原始发布时间 |
| collectedAt | datetime | 是 | 采集时间 |
| contentHash | string | 是 | 规范字段哈希 |
| generated | boolean | 是 | 是否含机器生成内容 |
| generatorVersion | string/null | 否 | 翻译/摘要生成器版本 |
| verificationStatus | enum | 是 | unreviewed、auto_checked、human_verified、rejected |
| originalAccessStatus | enum | 是 | unknown、reachable、slow、unreachable |
| legalBasis | enum | 是 | official_api、rss_allowed、public_notice、licensed、manual_link |

一级栏目稳定枚举：

1. policy_regulation
2. drug_rd
3. research_academic
4. pharma_biotech
5. hospital_service
6. device_diagnostics
7. capital_transactions
8. reimbursement_access
9. manufacturing_supply
10. quality_safety
11. public_health
12. frontier_technology

## 4. Feed静态契约

V1输出：

```text
feed-manifest.json
latest.json
daily/YYYY-MM-DD.json
taxonomy.json
source-health.json
```

`feed-manifest.json`包含schemaVersion、generatedAt、latestDate、availableDates、文件SHA-256和数据新鲜度。V2导入V1数据时通过相同契约读取。

## 5. API契约

V2起提供：

```text
GET    /api/v1/health
GET    /api/v1/articles
GET    /api/v1/articles/{id}
GET    /api/v1/taxonomy
GET    /api/v1/sources
POST   /api/v1/auth/login
POST   /api/v1/auth/logout
GET    /api/v1/me
GET    /api/v1/me/favorites
PUT    /api/v1/me/favorites/{articleId}
DELETE /api/v1/me/favorites/{articleId}
GET    /api/v1/me/follows
POST   /api/v1/me/follows
DELETE /api/v1/me/follows/{id}
```

管理接口统一位于`/api/v1/admin/**`。分页使用游标或稳定的`publishedAt,id`排序，不能在翻页过程中出现明显重复和遗漏。

## 6. 国内外采集策略

### 国内来源

优先从境内运行节点采集；官方页面无API/RSS时，采用可配置HTML解析器并保留fixture。

### 国外来源

- V1由GitHub Actions采集；
- V2由本地节点先尝试，失败来源由Actions补充；
- V3由独立境外采集节点处理，境内服务不在用户请求链路中实时访问国外来源。

不得使用Tailscale作为绕过网络限制的工具。Tailscale仅用于V2用户访问私有服务。

## 7. 版本迁移

### V1 → V2

1. 创建MySQL/Flyway基线；
2. 导入V1 Article JSON并保持原`id`；
3. 校验记录数、来源数、内容哈希和发布日期分布；
4. 前端把`VITE_DATA_MODE`切换为`api`；
5. IndexedDB收藏首次登录后由用户确认是否上传；
6. 保留静态导出能力作为只读回退。

### V2 → V3

1. 对数据库进行备份恢复演练；
2. 通过Flyway升级，不重建生产库；
3. 将用户文件和允许保存的素材迁移到对象存储；
4. 增加公网域名、HTTPS、WAF/限流和监控；
5. 默认开放匿名只读，管理和个性化功能继续鉴权；
6. 完成ICP备案、隐私和新闻信息服务合规评估后再开放。

## 8. 可用性和降级

- 任何单一来源失败：首页仍可使用；
- 翻译失败：显示原始标题并标记“待翻译”；
- 摘要失败：显示来源提供的短描述或空状态；
- 国外原文不可访问：保留结构化事实摘要和原始URL，不复制全文；
- 数据超过预期更新时间：显示“数据可能已过期”；
- V2/V3 API不可用：前端可选择展示最近一次合法静态快照。

## 9. 测试分层

- 领域单元测试：分类、规范化、去重、状态判定；
- 连接器契约测试：固定fixture输入产生稳定Article输出；
- JSON Schema/OpenAPI兼容测试；
- API集成测试：Testcontainers MySQL；
- 前端组件和Store测试；
- Playwright移动端E2E；
- 部署冒烟测试：健康检查、首页、列表、详情和登录。

## 10. 关键质量指标

| 指标 | V1目标 | V2目标 | V3目标 |
|---|---:|---:|---:|
| 定时采集成功率 | ≥95% | ≥97% | ≥99% |
| 核心来源数据延迟 | ≤6小时 | ≤2小时 | ≤30分钟（按来源能力） |
| 重复内容率 | <5% | <3% | <2% |
| 移动端关键页面可用率 | 人工验收 | ≥99%私有运行期 | ≥99.5%月度目标 |
| 原始来源可追溯率 | 100% | 100% | 100% |
| 未授权全文入库 | 0 | 0 | 0 |
