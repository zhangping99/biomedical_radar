# V1.0 来源目录与采集策略

## 结论

V1.0 当前登记 52 个官方或机构自有来源，其中 39 个已启用、13 个保留为禁用候选。2026-09-09 增加 10 个栏目，重点补充中国政策、国内医院和药企。中文摘要服务按用户要求暂不实施，沿用原有无密钥降级逻辑。

采集器只保存标题、短描述、时间、来源实体和原文链接，不下载论文全文、不转载新闻全文，也不绕过登录、验证码、WAF、付费墙或反爬限制。禁用候选保留在 `config/sources.yml` 中，是为了记录覆盖缺口和接入阻塞，不会进入定时任务。

## 分组与定时

GitHub Actions 使用 UTC；下表同时列出北京时间。手动运行可选择单组或 `all`。

| 分组 | 启用数 | 内容 | UTC 定时 | 北京时间 | 设计理由 |
|---|---:|---|---|---|---|
| `rapid` | 10 | 安全警示、召回、监管动态、公共卫生 | `17 */4 * * *` | 每 4 小时的 `00:17/04:17/08:17/12:17/16:17/20:17` | 高时效，但不承诺实时 |
| `policy` | 5 | 药审指导、医保、卫生政策及解读 | `47 */6 * * *` | `02:47/08:47/14:47/20:47` | 每六小时，错峰 |
| `research` | 8 | 注册库、论文索引、期刊、预印本 | `17 2,14 * * *` | `10:17/22:17` | 每天两次 |
| `pharma` | 11 | 国内外药企官方公告 | `37 1,7,13,19 * * *` | `03:37/09:37/15:37/21:37` | 每六小时 |
| `hospital` | 5 | 医院科研与临床服务 | `37 3,15 * * *` | `11:37/23:37` | 每天两次 |

旧参数 `institutional` 保留为 `pharma,hospital` 的别名，不再有独立定时任务。调度为期望触发时间，GitHub Actions 可能延迟。

### 2026-09-09 新增栏目

| ID | 官方栏目 | 分组 | 单次上限 |
|---|---|---|---:|
| nature-biotechnology | Nature Biotechnology RSS | research | 30 |
| nature-reviews-cancer | Nature Reviews Cancer RSS | research | 30 |
| nhsa-policy-explain | 国家医保局政策解读 | policy | 20 |
| guangdong-health-explain | 广东省卫生健康委政策解读 | policy | 20 |
| pkuh6-research | 北京大学第六医院科研新闻 | hospital | 20 |
| bjcancer-research | 北京大学肿瘤医院科研成果 | hospital | 20 |
| hansoh-news | 翰森制药新闻发布 | pharma | 20 |
| junshi-news | 君实生物新闻 | pharma | 20 |
| 3sbio-news | 三生制药新闻 | pharma | 20 |
| abbvie-news | AbbVie 官方新闻 RSS | pharma | 20 |

准确 URL 与选择器以 `config/sources.yml` 为准。公开列表与 RSS 已进行 HTTP 检查；HTTP 可访问不等于完整版权授权，持续遵守元数据范围并复核使用条件。测试样例均为自制最小结构，不保存第三方全文。长期不更新的栏目可能因 90 天保留期而暂时无文章，不把采集时间伪装为已知发布时间。

PubMed 改为近七天临床试验、Meta 分析和系统综述，上限 40；Europe PMC 限定癌症、糖尿病、免疫治疗、药物或临床主题，上限 40。医院和药企排除明确的招聘、采购、招标、会议/放假通知及预告型投资者会议标题，保留研究结果和临床招募。首页五个重点入口使用来源类型、地区、医院实体和学术分类组合，不因药企新闻被分到“药物研发”而漏掉。

同一时间只允许一个采集工作流写静态数据。单批全局并发上限为 4，同一域名最多 1 个请求；这既缩短总耗时，也避免 FDA 等同域多来源被并发冲击。

## 已启用来源

### 快速信号 `rapid`

| 配置 ID | 地区/类型 | 接入方式 | 单次上限 |
|---|---|---|---:|
| `fda-drug-news` | 美国药品监管 | FDA 官方 RSS | 20 |
| `fda-medwatch-alerts` | 美国安全警示 | FDA MedWatch 官方 RSS | 20 |
| `fda-biologics-news` | 美国疫苗、血液与生物制品 | FDA 官方 RSS | 20 |
| `ema-news` | 欧盟药品监管 | EMA 官方 RSS | 20 |
| `mhra-drug-device-alerts` | 英国药品与器械安全 | GOV.UK 官方 Atom | 20 |
| `pmda-safety-updates` | 日本上市后安全 | PMDA 官方 HTML 列表 | 20 |
| `health-canada-health-products` | 加拿大健康产品召回 | Health Canada 官方 HTML 列表 | 20 |
| `who-news` | 全球公共卫生 | WHO 官方 RSS | 20 |
| `china-cdc-news` | 中国公共卫生 | 中国疾控中心官方 HTML 列表 | 20 |
| `nmpa-public-notices` | 中国药品监管 | NMPA 英文官方 HTML 列表 | 20 |

### 政策与准入 `policy`

| 配置 ID | 地区/类型 | 接入方式 | 单次上限 |
|---|---|---|---:|
| `cde-drug-review-updates` | 中国药审与指导原则 | CDE 官方 HTML 列表 | 20 |
| `nhsa-healthcare-security-updates` | 中国医保政策法规 | 国家医保局官方 HTML 内嵌记录 | 20 |
| `beijing-health-policy` | 中国卫生政策与医疗管理 | 北京市卫健委官方 HTML 列表 | 20 |

### 学术与临床研究 `research`

| 配置 ID | 范围 | 接入方式 | 可信度处理 |
|---|---|---|---|
| `clinicaltrials-recent` | 全球临床试验登记 | ClinicalTrials.gov API v2，仅读取 4 组必要字段 | A 级注册库 |
| `europe-pmc-recent` | 全球论文元数据 | Europe PMC REST API | A 级索引，不取全文 |
| `pubmed-recent` | 全球生物医学论文元数据 | NCBI ESearch + ESummary | A 级索引，两次同域请求 |
| `nature-medicine` | 医学研究 | Nature Medicine 官方 RSS | A 级期刊来源 |
| `nejm-current-issue` | 临床医学研究 | NEJM 官方 RSS | A 级期刊来源 |
| `medrxiv-recent` | 医学预印本 | medRxiv 官方 RSS | B 级且强制 `verificationStatus=unreviewed` |

### 原有医院与药企（现分别属于 `hospital`、`pharma`）

| 配置 ID | 地区/类型 | 接入方式 | 来源级别 |
|---|---|---|---|
| `pumch-research` | 中国医院科研 | 北京协和医院官方 HTML 列表 | B |
| `btch-research` | 中国医院科研 | 北京清华长庚医院官方 HTML 列表 | B |
| `cleveland-clinic-news` | 美国医院科研 | Cleveland Clinic 官方 HTML 列表 | B |
| `amgen-news-releases` | 美国药企 | Amgen 官方公开 JSON 接口 | B |
| `pfizer-press-releases` | 美国药企 | Pfizer 官方 HTML 列表 | B |
| `gsk-press-releases` | 英国药企 | GSK 官方 RSS | B |
| `lilly-news-releases` | 美国药企 | Lilly 官方 RSS | B |
| `merck-news-releases` | 美国药企 | Merck 官方 HTML 列表 | B |
| `sanofi-press-releases` | 欧盟药企 | Sanofi 官方 HTML 列表 | B |
| `hengrui-focus-news` | 中国药企 | 恒瑞医药官方 HTML 列表 | B |

监管机构、注册库、论文索引和正式期刊使用 A 级；医院科研、企业新闻稿和预印本使用 B 级。B 级表示机构自述或尚未同行评议，并不表示内容错误；它们的“获批、达到终点”等表述仍需回到监管文件或论文核验。企业来源不再仅因“官方公司网站”就获得 A 级加分。

## 禁用候选

| 配置 ID | 希望补齐的覆盖 | 当前不启用的原因 |
|---|---|---|
| `nhc-policy-candidate` | 国家卫健委政策 | 无人值守请求返回 HTTP 412；不绕过限制 |
| `nmpa-chinese-candidate` | NMPA 中文监管 | 无人值守请求返回 HTTP 412；英文 NMPA 来源已启用 |
| `china-drug-trials-candidate` | 中国药物临床试验 | 公共检索页返回客户端挑战，未确认稳定元数据接口 |
| `chictr-candidate` | 中国临床试验注册 | 验证时公共检索端点返回 HTTP 405 |
| `mayo-clinic-candidate` | 美国医院科研 | 官方 Feed 对无人值守请求返回 HTTP 403 |
| `west-china-hospital-candidate` | 中国医院科研 | TLS/网络连接在验证期间不稳定 |
| `huashan-hospital-candidate` | 中国医院科研 | TLS/网络连接在验证期间不稳定 |
| `astrazeneca-candidate` | 跨国药企 | 官方页面对无人值守请求返回 HTTP 403 |
| `roche-candidate` | 跨国药企 | 列表由前端动态加载，尚无已确认的稳定公开端点 |
| `beone-candidate` | 跨国/中国创新药企 | 投资者列表由前端动态加载，尚无已确认的稳定 Feed |
| `wuxi-apptec-candidate` | 中国医药研发服务企业 | 列表由前端动态加载，内部接口契约未确认 |
| `fosun-pharma-candidate` | 中国药企 | 列表动态加载且网络访问间歇不稳定 |
| `novartis-candidate` | 跨国药企 | Feed 目录存在，但生产订阅端点及使用边界尚待确认 |

候选来源只有在以下条件全部满足后才能改为 `enabled: true`：确认官方稳定 URL 和公开访问边界；添加最小 fixture；解析到至少一条合法元数据；单源失败隔离测试通过；连续试采无验证码、登录或绕过行为。

## 采集保护与失败语义

- 默认最大并发 4，可通过 `--max-concurrency=1..8` 调整；同域名始终串行。
- 每个请求设置 20–30 秒超时，最多 3 次有限重试；4xx（429 除外）不重复冲击来源。
- 单响应超过约 5 百万字符即拒绝；一般来源每批10–20条，新增期刊最多30条，定向论文索引最多40条，避免超大Feed扩散。
- 页面返回 HTTP 200 但解析不到条目时记为 `NO_ITEMS` 失败，防止页面改版或挑战页被误报为健康。
- 单个来源失败不会中止其他来源；仅当所选分组全部失败时任务失败。
- 分组运行会合并既有 `source-health.json`，更新本组状态并保留其他组最近状态；已禁用或删除的来源会被清理。
- 重复运行保持幂等；新批次和既有 `latest.json` 合并、去重后保留最近 90 天。
- HTML 日期支持 ISO、斜杠、点号、中文年月日和英文月份；URL 含日期的官方列表可从 URL 提取发布日期。

## 2026-09-08 本机真实试采

| 分组 | 结果 | 说明 |
|---|---|---|
| `rapid` | 9/10 成功 | NMPA 在本机 Java 17 仍为 `SSLHANDSHAKEEXCEPTION`；此前 GitHub Actions 对该来源成功，继续以来源健康状态观察环境差异 |
| `policy` | 3/3 成功 | CDE 首页重复展示的 10 条记录被来源内去重；医保局 CDATA 列表按记录片段解析 |
| `research` | 6/6 成功 | 共抓取 95 条元数据；PubMed 两阶段请求成功 |
| `institutional` | 10/10 成功 | 共抓取 111 条医院和药企元数据 |

真实试采只用于验证公开入口和解析规则，不等同于完成版权、使用条款或长期稳定性审查。

## 2026-09-08 GitHub 全量采集

[远程运行 34189267105](https://github.com/zhangping99/biomedical_radar/actions/runs/34189267105) 已完成采集、数据契约校验、前端门禁、静态数据提交和 GitHub Pages 部署：合并最近 90 天后发布 341 条文章和 59 个静态文件，29 个启用来源中 28 个成功。

本次唯一失败是 `nejm-current-issue` 返回 `HTTP_403`；NMPA 在 GitHub 环境采集成功。工作流按设计发布 `freshness=partial`，不影响其他来源与已有合法记录，并在 `source-health.json` 中记录失败和下次重试时间。线上首页、manifest 和 latest 均返回 HTTP 200，`latest.json` 的 595591 字节大小与 SHA-256 已和线上 manifest 完整核对。

首次全量运行暴露出 medRxiv RSS 链接两侧含换行的问题，契约门禁阻止了无效数据发布；采集器随后在 Article 映射边界统一清理 URL 首尾空白，并补充真实形态 fixture 回归。修复后的全量运行才被允许提交和部署。

## 官方入口与接口依据

- [FDA RSS](https://www.fda.gov/about-fda/contact-fda/stay-informed/rss-feeds)
- [EMA RSS](https://www.ema.europa.eu/en/news-events/rss-feeds)
- [MHRA 药品与器械警示](https://www.gov.uk/drug-device-alerts)
- [PMDA 上市后安全信息](https://www.pmda.go.jp/english/safety/0001.html)
- [Health Canada 健康产品召回](https://recalls-rappels.canada.ca/en/search/site?f%5B0%5D=cat%3A180)
- [WHO RSS](https://www.who.int/about/communications)
- [ClinicalTrials.gov API](https://clinicaltrials.gov/data-api/api)
- [Europe PMC REST API](https://europepmc.org/RestfulWebService)
- [NCBI E-utilities](https://www.ncbi.nlm.nih.gov/books/NBK25501/)
- [Nature Medicine RSS](https://www.nature.com/nm.rss)
- [NEJM RSS](https://www.nejm.org/action/showFeed?type=etoc&feed=rss&jc=nejm)
- [medRxiv RSS](https://connect.medrxiv.org/medrxiv_xml.php?subject=all)
- [国家药审中心](https://www.cde.org.cn/)
- [国家医保局政策法规](https://www.nhsa.gov.cn/col/col53/index.html)
- [中国疾病预防控制中心](https://www.chinacdc.cn/)
- [北京协和医院科研](https://www.pumch.cn/research/)
- [北京清华长庚医院科研](https://www.btch.edu.cn/jxky/)
- [恒瑞医药新闻](https://www.hengrui.com/media/NewsType-1.html)
