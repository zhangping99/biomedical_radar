# 医药雷达项目指令

本文件适用于整个仓库。Codex开始任何任务前必须先阅读本文件，然后读取`docs/CURRENT_PHASE.md`以及其中指定的当前阶段PRD。若任务只涉及某个子目录，还应先阅读该目录下更具体的`AGENTS.md`（如存在）。

## 1. 产品目标

医药雷达是一款面向中国用户的移动端医药行业信息工具，覆盖国内外监管、药品研发、科研、医院、药企、器械诊断、投融资、医保准入、供应链、质量安全、公共卫生和前沿技术信息。

核心体验必须同时支持：

- iPhone Safari及安装后的PWA；
- Android Chrome及安装后的PWA；
- 中文优先展示，保留原文标题、来源和链接；
- 原文不可访问时，仍能阅读已合法保存的结构化事实摘要；
- 每条信息都可追溯到来源和采集时间。

## 2. 版本纪律

- 当前阶段以`docs/CURRENT_PHASE.md`为唯一依据。
- 只实现当前阶段PRD中的P0范围，不得提前引入下一阶段基础设施。
- V1不得启动常驻后端、MySQL、用户账号或公网写接口。
- V2不得开放匿名公网访问，不得把MySQL、管理端口或Spring Boot端口直接暴露到公网。
- V3未通过合规上线门禁前，不得宣称或配置为可公开运营。
- 后续版本必须通过兼容迁移扩展V1的数据契约；不要推翻前端、核心领域模型或来源适配器。

## 3. 固定技术栈

### 前端

- Vue 3、TypeScript、Vite；
- Vue Router、Pinia；
- PWA manifest和Service Worker；
- Vitest用于单元测试，Playwright用于关键移动端流程；
- 不从外部CDN加载JavaScript、CSS、字体或关键图标；
- 样式必须采用mobile-first，并使用CSS安全区域变量适配刘海屏和底部手势区。

### 后端与采集

- Java 17 LTS；
- Spring Boot使用与Java 17兼容的稳定版本并在父POM中锁定；
- Maven Wrapper；
- V1采集器以非Web命令行模式运行并输出静态JSON；
- V2/V3启用Spring MVC、Spring Security、Bean Validation、Spring Data JPA和Actuator；
- V2/V3使用MySQL 8.x和Flyway，禁止依赖Hibernate自动修改生产表结构；
- 所有外部来源都通过`SourceConnector`适配器访问，不允许在Controller或页面组件中直接抓取来源。

## 4. 推荐仓库结构

```text
frontend/                 Vue 3 PWA
backend/
  radar-domain/           核心领域模型和规则
  radar-collector/        RSS/API/网页来源适配器与静态导出
  radar-api/              V2起启用的REST API
contracts/                JSON Schema与OpenAPI契约
config/sources.yml        来源配置，不包含密钥
data/fixtures/            合法、最小化、脱敏的测试样本
infra/                    Actions、Docker、Nginx及部署配置
docs/                     PRD、架构、决策和运行手册
```

如现有仓库结构不同，先说明映射关系，再做最小调整，不得为了匹配此示例而无理由大规模移动文件。

## 5. 数据与内容规则

- 使用`contracts/article.schema.json`作为跨版本Article契约的机器可读基准。
- 时间统一保存为UTC ISO-8601，界面按Asia/Shanghai显示。
- 分类、地区、事件、疾病领域、对象类型使用稳定枚举或字典表，不使用散落的自由文本常量。
- 去重至少组合使用规范化URL、来源ID、标题指纹和事件实体。
- 保存原始来源、原始标题、发布时间、采集时间和内容哈希。
- 默认不保存或发布第三方新闻全文、付费内容、受限图片或附件。
- 不绕过登录、验证码、付费墙、robots限制、地区限制或反爬措施。
- 优先接入官方API、RSS、监管公告、临床试验注册和论文元数据。
- AI或机器生成的标题翻译、摘要和影响说明必须带`generated=true`及生成器版本；生成失败不得阻断新闻入库。
- 不生成诊断、处方、个体化用药或证券交易建议。

## 6. 安全规则

- 密钥只通过环境变量、GitHub Secrets或部署平台秘密管理传入；不得提交`.env`、密码、Token、私钥或真实用户数据。
- 日志不得记录密码、Session、Authorization头、完整Token或不必要的个人信息。
- V2/V3默认同源部署；浏览器登录使用Secure、HttpOnly、SameSite Cookie和CSRF防护。
- 数据库只允许后端网络访问；管理接口必须鉴权并按角色授权。
- 所有外部HTTP调用必须设置连接/读取超时、有限重试、速率限制标识和可观测错误码。
- 添加生产依赖前说明必要性、许可证、维护状态和替代方案。

## 7. 工程约定

- 优先小而可审查的改动；不要在一个任务中同时实现多个阶段。
- Java包按领域组织；Controller不得包含采集、翻译或持久化业务逻辑。
- TypeScript启用严格模式，禁止无理由使用`any`。
- API统一前缀为`/api/v1`，错误响应遵循RFC 9457 Problem Details风格。
- 数据库变更必须新增Flyway迁移，已经执行的迁移文件不得修改。
- 外部来源测试使用本地fixture或MockWebServer/WireMock；测试不得依赖实时互联网。
- 用户可见中文文案应清楚、克制，并区分“获批、受理、推荐批准、临床达到终点”等状态。

## 8. 必须执行的验证

修改前端后，在`frontend/`运行：

```bash
pnpm install --frozen-lockfile
pnpm run lint
pnpm run test
pnpm run build
```

修改后端后，在`backend/`运行：

```bash
./mvnw verify
```

修改部署或跨端流程后，运行当前阶段PRD指定的集成/E2E检查。若环境不具备运行条件，明确记录未运行的命令和原因，不得声称通过。

## 9. 完成定义

每个任务结束前必须：

1. 对照当前PRD的需求ID和验收条件检查范围；
2. 补充或更新自动化测试；
3. 运行相关lint、测试和构建；
4. 检查差异中是否包含秘密、生成垃圾、第三方全文或不必要依赖；
5. 更新受影响的契约、运行说明和`docs/CURRENT_PHASE.md`清单；
6. 在结果中列出已完成项、改动文件、验证结果、未完成风险和下一步。

## 10. 遇到不确定事项

- 对产品范围、合规结论、数据许可、付费服务、公开网络暴露和破坏性迁移，不得自行假设，必须先询问。
- 对可逆的代码组织和实现细节，可作最小合理假设，并在结果中记录。
- 如果来源不可访问，记录失败并降级，不得尝试规避网络限制。
