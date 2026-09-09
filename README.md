# 医药雷达

个人使用的医药行业静态信息 PWA。V1.0 通过 Java 17 命令行采集器读取公开官方元数据，生成带 SHA-256 的静态 JSON；Vue 3 应用部署到 GitHub Pages，收藏、已读和关注仅保存在浏览器 IndexedDB。项目不运行常驻后端。

> 当前阶段锁定为 **V1.0 P0**。V2/V3 只保留 Article 数据契约、`ArticleRepository` 边界和 `radar-api` 目录，不包含数据库、登录、REST API、Docker 或公网生产设施。

## 已实现范围

- 52 个来源配置：39 个启用来源覆盖国内外监管、政策、学术、医院和药企，13 个受访问限制或缺少稳定接口的来源保留为禁用候选；
- `rapid`、`policy`、`research`、`pharma`、`hospital` 五组定时策略；旧 `institutional` 参数兼容映射到医院和药企两组；
- 首页提供学术研究、中国政策、国内药企、国外药企、国内医院五个重点入口；今日必读每来源最多两条，完整最新流保持时间排序；
- 全局并发上限、同域名串行、有限重试、响应大小/单批条数限制、空页面检测和来源级失败隔离；
- URL/标题规范化、稳定 ID、内容哈希、保守去重、12 个栏目、事件识别和配置化重要度评分；
- 无密钥降级翻译/摘要 Provider，不编造外文中文内容；
- `feed-manifest.json`、`latest.json`、每日文件、taxonomy、来源健康报告和 SHA-256；
- 今日、分类、详情、关注、收藏、设置六个移动端页面；
- 搜索、组合筛选、IndexedDB、备份导入/导出、深色模式、离线缓存和 PWA 更新提示；
- CI、分组错峰定时采集、验证后提交静态数据、GitHub Pages 部署和回滚说明。

产品范围见 [V1 PRD](docs/PRD_V1_PERSONAL_STATIC.md)，当前验收状态见 [CURRENT_PHASE](docs/CURRENT_PHASE.md)，来源选择见 [SOURCES](docs/SOURCES.md)。旧版需求已移至 `docs/archive`，不再作为实现依据。

## 目录

```text
backend/
  radar-domain/       Article、规范化、去重和评分
  radar-collector/    Spring Boot 非 Web 命令行采集与导出
  radar-api/          V2 边界说明，当前不参与构建
frontend/             Vue 3 + TypeScript + Vite PWA
contracts/            Article、Feed、来源健康 JSON Schema
config/               来源和评分配置
data/fixtures/        自制的契约与连接器测试样例
infra/                V1 Pages 回滚说明
.github/workflows/    CI、采集、Pages 部署
```

## 环境

- Java 17；
- 项目自带 Maven Wrapper；
- Node.js 24；
- pnpm 9.12.3。

本机已约定使用 Maven 设置 `J:\apache-maven-3.6.3\conf\settings.xml` 和本地仓库 `H:\work_code\repository\blade440`。这些机器专属路径不写入 GitHub Actions。

## Windows PowerShell

首次安装前端依赖并生成图标：

```powershell
Set-Location H:\pre_project\biomedical_radar\frontend
pnpm install --frozen-lockfile
pnpm generate:icons
```

验证后端（使用约定的 settings 和本地仓库）：

```powershell
Set-Location H:\pre_project\biomedical_radar\backend
$env:MAVEN_OPTS = '-Dmaven.repo.local=H:\work_code\repository\blade440'
.\mvnw.cmd -s 'J:\apache-maven-3.6.3\conf\settings.xml' verify
```

使用固定 fixture 生成可复现的本地数据：

```powershell
Set-Location H:\pre_project\biomedical_radar
java -jar backend\radar-collector\target\radar-collector-1.0.0-SNAPSHOT.jar `
  --sources=config\sources.yml `
  --scoring=config\scoring.yml `
  --fixture-dir=data\fixtures\sources `
  --output-dir=frontend\public\data `
  --groups=all `
  --max-concurrency=4
```

启动或完整验证前端：

```powershell
Set-Location H:\pre_project\biomedical_radar\frontend
pnpm dev

# 完整质量门禁
pnpm validate:data
pnpm lint
pnpm test
pnpm build
pnpm exec playwright install chromium webkit
pnpm test:e2e
```

## macOS / Linux / Git Bash

```bash
cd backend
./mvnw verify
cd ..
java -jar backend/radar-collector/target/radar-collector-1.0.0-SNAPSHOT.jar \
  --sources=config/sources.yml \
  --scoring=config/scoring.yml \
  --fixture-dir=data/fixtures/sources \
  --output-dir=frontend/public/data \
  --groups=all \
  --max-concurrency=4
cd frontend
pnpm install --frozen-lockfile
pnpm validate:data && pnpm lint && pnpm test && pnpm build
```

去掉 `--fixture-dir` 即执行真实公开来源采集。`--groups` 可取 `all`、`rapid`、`policy`、`research`、`pharma`、`hospital`，也可使用逗号组合；旧 `institutional` 等同 `pharma,hospital`。不传时采集全部启用来源。请先阅读 `docs/SOURCES.md`，不要提高频率或绕过来源限制。

## GitHub Pages

线上验证地址：[https://zhangping99.github.io/biomedical_radar/](https://zhangping99.github.io/biomedical_radar/)。首次部署已于 2026-09-08 完成验收；页面、静态数据、SHA-256、Chromium、WebKit 和 Service Worker 均通过线上冒烟。

1. 推送代码后，在仓库 Settings → Pages 中选择 **GitHub Actions**；
2. 手动运行一次 `CI`，成功后 `Deploy GitHub Pages` 会部署同一提交；
3. 手动运行 `Collect static feed` 验证真实来源；任务会在契约、测试和构建全部通过后提交静态数据，并直接部署同一份已验证产物；

快速安全与监管信号每四小时采集，中国政策和药企每六小时采集，学术和医院每天两次；五组任务错峰且共用单写入并发锁。任何构建、契约或测试失败都会阻止新版本部署，上一版 Pages 保持不变。回滚流程见 [infra/README.md](infra/README.md)。

## 安全与内容边界

- 不提交 Token、Cookie、私钥或来源响应正文；秘密只通过 GitHub Secrets 或本机环境变量提供；
- 只发布标题、元数据、短摘要和原文链接，不保存或转载第三方全文；
- C 级来源不会进入今日必读；解析失败只进入脱敏健康报告；
- 机器生成字段带 `generated`、版本与时间标识；默认无密钥模式不会伪造翻译；
- 内容仅供个人行业研究，不构成医疗、用药或投资建议。

## 尚需人工/外部验证

- 扩源后的五组定时采集各连续成功七次（当前从新计划重新累计）；
- 真机 iPhone Safari/PWA 与 Android Chrome/PWA 安装、弱网和离线验收；
- NMPA HTTPS 在本机 Java 17 中曾发生证书握手失败，但扩源后的 GitHub Actions 全量采集成功；NEJM 官方 Feed 在该次远程采集中返回一次 `HTTP_403`，两者继续按来源健康状态观察；
- 对 39 个启用来源的版权、使用条件和页面结构做定期复核；13 个禁用候选不得在未确认公开访问边界前启用；
- 14 天个人使用价值观察。以上项目完成前，`CURRENT_PHASE.md` 不得切换到 V2。
