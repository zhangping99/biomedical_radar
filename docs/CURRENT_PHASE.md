# 当前开发阶段

## 当前版本

`V1.0 - 个人静态验证版`

生效PRD：`docs/PRD_V1_PERSONAL_STATIC.md`

后续PRD：

- `docs/PRD_V2_PRIVATE_SELF_HOSTED.md`
- `docs/PRD_V3_PUBLIC_CLOUD.md`

## 阶段规则

- Codex默认只实现V1.0的P0需求。
- 除非用户明确要求切换版本，不实现V2/V3功能。
- V1验收通过后，先填写本文件的升级检查表，再把当前版本改为V2.0。
- 不允许跳过V2直接把V1部署为公开生产服务。

## V1进度清单

- [x] 仓库骨架和开发工具完成
- [x] Article与Feed JSON Schema完成
- [x] 来源配置及至少6个连接器完成（fixture全通过；本机真实采集5/6成功）
- [x] 去重、分类、静态导出完成
- [x] Vue 3移动端PWA主要页面完成
- [x] 搜索、筛选、收藏和关注完成
- [x] GitHub Actions定时采集完成（截至2026-09-08已连续成功3次，继续观察至7次）
- [x] GitHub Pages部署完成（2026-09-08已验证线上地址、数据哈希和双浏览器访问）
- [ ] iPhone Safari/PWA真机验收完成（Playwright WebKit模拟已通过）
- [ ] Android Chrome/PWA真机验收完成（Playwright Chromium模拟已通过）
- [ ] 内容来源与版权检查完成
- [x] V1本地自动化回归测试全部通过

## V1升V2门槛

- [ ] 连续14天定时任务成功率达到95%以上
- [ ] 核心来源重复率低于5%
- [ ] 每条记录均可追溯到原始来源
- [ ] 用户确认分类、摘要和今日必读有持续使用价值
- [ ] 已确定持续运行的Windows/NAS/迷你主机
- [ ] 已完成MySQL备份与恢复演练方案
- [ ] 已确定不超过6人的私有试用名单

## 决策记录

如变更技术栈、范围或阶段，应在此追加：

```text
日期：YYYY-MM-DD
决策：
原因：
影响的需求ID：
迁移或回滚方式：
```

日期：2026-09-07  
决策：V1至V3的Java基线由Java 21调整为Java 17，当前仅实现V1.0 P0。  
原因：用户明确指定使用本机已有的Java 17，减少个人项目的环境与维护成本。  
影响的需求ID：V1全部后端任务；后续V2/V3 Java代码继续保持Java 17兼容。  
迁移或回滚方式：父POM通过`maven.compiler.release=17`锁定；如未来升级JDK，先在独立分支完成全量验证再更新本文件与`AGENTS.md`。

日期：2026-09-07  
决策：前端包管理器使用pnpm 9.x并提交`pnpm-lock.yaml`。  
原因：本机全局npm存在已确认的`minipass-flush`依赖树损坏，npm在解析项目依赖前即退出；pnpm 9.12.3在同一环境可用。  
影响的需求ID：V1全部前端任务和CI。  
迁移或回滚方式：CI通过Corepack启用锁定版本；全局npm修复后，如需切回npm，先重新生成并验证`package-lock.json`，再统一修改CI和`AGENTS.md`，禁止同时保留两套锁文件。

日期：2026-09-07  
决策：企业公告正式来源采用Amgen官方公开JSON接口；SEC解析器仅保留为兼容能力，不在V1默认来源中启用。  
原因：SEC submissions接口与Pfizer新闻站在当前无人值守网络环境分别返回403和Cloudflare挑战；Amgen接口无需密钥且可稳定读取，符合不绕过访问限制的原则。  
影响的需求ID：V1-F-008、V1-F-009、V1-F-013。  
迁移或回滚方式：来源通过`config/sources.yml`切换；Article契约和前端不受影响，恢复其他企业源时只需新增fixture并通过连接器回归测试。

日期：2026-09-08
决策：确认V1个人验证站点已部署到GitHub Pages，但继续保持V1.0阶段，不视为V2/V3公共生产发布。
原因：远程CI、Pages部署、线上静态数据哈希以及Chromium/WebKit移动视口冒烟均已通过；连续7次采集、真机安装和14天价值观察尚未完成。
影响的需求ID：V1-A-001、V1-A-002、V1-A-004、V1-A-005、V1-A-006、V1-A-012。
迁移或回滚方式：站点仍由静态产物和GitHub Actions发布；失败时按`infra/README.md`回滚，不引入常驻后端或V2能力。
