# 医药雷达

个人使用的医药行业静态信息 PWA。V1.0 通过 Java 17 命令行采集器读取公开官方元数据，生成带 SHA-256 的静态 JSON；Vue 3 应用部署到 GitHub Pages，收藏、已读和关注仅保存在浏览器 IndexedDB。项目不运行常驻后端。

> 当前阶段锁定为 **V1.0 P0**。V2/V3 只保留 Article 数据契约、`ArticleRepository` 边界和 `radar-api` 目录，不包含数据库、登录、REST API、Docker 或公网生产设施。

## 已实现范围

- 六个来源配置：FDA、EMA、ClinicalTrials.gov、Europe PMC、Amgen、NMPA；
- RSS、REST API、HTML 公告三类接入框架，有限重试、超时和来源级失败隔离；
- URL/标题规范化、稳定 ID、内容哈希、保守去重、12 个栏目、事件识别和配置化重要度评分；
- 无密钥降级翻译/摘要 Provider，不编造外文中文内容；
- `feed-manifest.json`、`latest.json`、每日文件、taxonomy、来源健康报告和 SHA-256；
- 今日、分类、详情、关注、收藏、设置六个移动端页面；
- 搜索、组合筛选、IndexedDB、备份导入/导出、深色模式、离线缓存和 PWA 更新提示；
- CI、四小时定时采集、验证后提交静态数据、GitHub Pages 部署和回滚说明。

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
  --output-dir=frontend\public\data
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
  --output-dir=frontend/public/data
cd frontend
pnpm install --frozen-lockfile
pnpm validate:data && pnpm lint && pnpm test && pnpm build
```

去掉 `--fixture-dir` 即执行真实公开来源采集。请先阅读 `docs/SOURCES.md`，不要提高频率或绕过来源限制。

## GitHub Pages

1. 推送代码后，在仓库 Settings → Pages 中选择 **GitHub Actions**；
2. 手动运行一次 `CI`，成功后 `Deploy GitHub Pages` 会部署同一提交；
3. 手动运行 `Collect static feed` 验证真实来源；任务会在契约、测试和构建全部通过后提交静态数据，并直接部署同一份已验证产物；
4. 站点地址预计为 `https://zhangping99.github.io/biomedical_radar/`。

定时任务每四小时运行。任何构建、契约或测试失败都会阻止新版本部署，上一版 Pages 保持不变。回滚流程见 [infra/README.md](infra/README.md)。

## 安全与内容边界

- 不提交 Token、Cookie、私钥或来源响应正文；秘密只通过 GitHub Secrets 或本机环境变量提供；
- 只发布标题、元数据、短摘要和原文链接，不保存或转载第三方全文；
- C 级来源不会进入今日必读；解析失败只进入脱敏健康报告；
- 机器生成字段带 `generated`、版本与时间标识；默认无密钥模式不会伪造翻译；
- 内容仅供个人行业研究，不构成医疗、用药或投资建议。

## 尚需人工/外部验证

- GitHub Pages 首次启用和线上地址；
- 定时采集连续七次成功；
- 真机 iPhone Safari/PWA 与 Android Chrome/PWA 安装、弱网和离线验收；
- NMPA HTTPS 在本机 Java 17 中证书握手失败，采集器会如实标记单源失败并继续发布其余来源；需在 GitHub Actions 环境复核；
- 对首批来源的版权、使用条件和页面结构做定期复核；
- 14 天个人使用价值观察。以上项目完成前，`CURRENT_PHASE.md` 不得切换到 V2。
