# V1.0 首批来源

## 结论

V1.0 配置六个不需要密钥的官方来源，覆盖中国、美国、欧盟及全球数据，并覆盖监管、临床试验、科研元数据和企业公告。采集器只读取公开列表、RSS 或官方 API 返回的元数据，不抓取付费墙、登录后内容或第三方全文。

## 来源清单

| 配置 ID | 覆盖 | 接入方式 | 选择理由 | 自动采集边界 |
|---|---|---|---|---|
| `fda-drug-news` | 美国监管 | FDA 官方 RSS | 一手药品监管动态，结构稳定 | 只保存条目元数据和原文链接 |
| `ema-news` | 欧盟监管 | EMA 官方 RSS | 一手欧盟药品监管动态 | 只保存条目元数据和原文链接 |
| `clinicaltrials-recent` | 全球临床试验 | ClinicalTrials.gov API v2 | 官方注册库、无密钥 | 查询公开研究元数据，控制页大小 |
| `europe-pmc-recent` | 全球科研 | Europe PMC REST API | 官方开放科研元数据服务 | 不下载论文全文 |
| `amgen-news-releases` | 企业公告 | Amgen 官方公开 JSON 接口 | 官方新闻稿；无需登录或密钥，适合无人值守采集 | 只保存新闻稿元数据和官方原文链接 |
| `nmpa-public-notices` | 中国监管 | NMPA 英文公开页面 HTML 列表 | 补齐境内官方监管来源 | 仅解析公开列表；页面结构变化时标记失败，不绕过限制 |

## 官方依据

- FDA RSS：<https://www.fda.gov/about-fda/contact-fda/stay-informed/rss-feeds>
- EMA RSS：<https://www.ema.europa.eu/en/news-events/ema-newsletters-rss-feeds>
- ClinicalTrials.gov API：<https://clinicaltrials.gov/data-api/api>
- Europe PMC REST API：<https://europepmc.org/RestfulWebService>
- Amgen 新闻稿列表：<https://www.amgen.com/newsroom/press-releases>
- NMPA 公开药品页面：<https://english.nmpa.gov.cn/drugs.html>

## 运行策略

- 默认每四小时执行一次，每个来源单次最多读取少量最新条目。
- 连接和读取有超时，暂时性错误最多尝试三次；单个来源失败不终止批次。
- 测试只使用 `data/fixtures/sources` 的短小自制样例，不依赖外网。
- 来源页面规则、使用条件或合法性不明确时，应先在 `config/sources.yml` 中设为 `enabled: false` 并记录原因。
- 来源健康报告不包含响应正文、Cookie、Token 或异常堆栈。
