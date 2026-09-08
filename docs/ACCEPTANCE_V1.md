# V1.0 P0 验收记录

## 结论

截至 2026-09-08，V1.0 P0 的代码、契约、本地回归、远程 CI 和 GitHub Pages 首次部署已完成，线上数据完整性与双浏览器移动视口冒烟均通过。当前状态是“已上线个人验证站点并持续观察”，不是“V1全部验收完成”：连续 7 次定时采集目前完成 3 次，两类真机安装测试、完整内容许可复核和 14 天价值观察仍需完成。

## 自动化验证结果

| 门禁 | 结果 | 证据范围 |
|---|---|---|
| Java 17 Maven `verify` | 通过 | `radar-domain` 3 项、`radar-collector` 4 项，共 7 项测试；包含六来源fixture、单源失败隔离、去重、评分、未来日期保护和过期日文件清理 |
| 静态数据契约 | 通过 | Article、Feed、source-health schema及SHA-256；当前发布目录共验证15个文件 |
| ESLint | 通过 | 0 warning门禁 |
| Vitest | 通过 | 4个测试文件、7项测试 |
| 生产构建 | 通过 | TypeScript检查、Vite构建、PWA manifest与Service Worker生成 |
| Playwright移动端 | 通过 | iPhone 13 WebKit与Pixel 7 Chromium共8项；覆盖加载/详情追溯、筛选/收藏、横向溢出和IndexedDB降级 |
| GitHub远程CI | 通过 | 2026-09-07远程CI完整执行Maven、fixture导出、双目录数据校验、ESLint、Vitest、构建及8项Playwright测试 |
| GitHub Pages线上冒烟 | 通过 | 页面及三个数据端点均为HTTP 200；线上`latest.json`大小和SHA-256与manifest一致；Chromium/WebKit均加载文章、注册Service Worker、无横向溢出且可追溯HTTPS原文 |
| 定时采集 | 观察中 | 截至2026-09-08连续3次计划任务成功；最新线上数据64条，6个来源状态均为`ok`，manifest为`fresh` |
| 真实来源试采 | 部分成功 | 本机试采50条，5/6来源成功且NMPA为`SSLHANDSHAKEEXCEPTION`；最新GitHub Actions采集6/6来源成功，需继续观察环境差异 |

## PRD验收项

| ID | 当前状态 | 说明 |
|---|---|---|
| V1-A-001 | 待线上观察 | 工作流和单源失败隔离已实现；GitHub Actions已连续成功3次，仍需达到7次 |
| V1-A-002 | 已通过 | 当前发布JSON通过Schema及文件哈希校验，线上`latest.json`也完成字节级SHA-256复核 |
| V1-A-003 | 已通过 | 跨来源保守去重与全部来源链接保留有领域测试 |
| V1-A-004 | 待真机 | 本地及线上WebKit移动视口回归通过；需iPhone添加主屏幕并独立启动 |
| V1-A-005 | 待真机 | 本地及线上Chromium移动视口回归通过；需Android安装并独立启动 |
| V1-A-006 | 部分通过 | 请求失败读取IndexedDB最近缓存已通过，线上Service Worker已注册；需真机飞行模式复核独立启动 |
| V1-A-007 | 部分通过 | 中英文搜索与组合筛选测试通过；1万条目标设备性能尚未压测 |
| V1-A-008 | 部分通过 | 收藏、IndexedDB与备份导入导出自动化通过；需真机重启浏览器复核 |
| V1-A-009 | 部分通过 | 最新线上数据64条且通过必填来源/时间/链接契约；尚不足100条人工抽检样本 |
| V1-A-010 | 部分通过 | fixture最小化、构建产物和差异敏感信息扫描已纳入交付检查；来源许可仍需定期人工复核 |
| V1-A-011 | 已通过 | 数据请求失败时保留缓存并展示明确状态，Playwright已覆盖 |
| V1-A-012 | 已通过 | manifest新鲜度及来源失败提示已实现；线上manifest为`fresh`且6个来源均为`ok` |

## 发布与人工清单

- [x] 在 GitHub 仓库 Settings → Pages 选择 GitHub Actions，并确认首次部署地址。
- [x] 远程 `CI` 完整通过，`Collect static feed` 在GitHub托管环境完成真实采集，NMPA最新一次为成功。
- [x] 线上页面、静态数据端点、字节级SHA-256及Chromium/WebKit移动视口冒烟通过。
- [ ] 记录连续7次采集结果；单源失败可接受，但不得全部来源失败或发布无效JSON。
- [ ] 分别在真实iPhone和Android上验证安装、独立窗口、弱网、飞行模式、返回滚动位置与44px点击目标。
- [ ] 对六个来源的使用条款、转载边界与页面结构做人工复核；只保留元数据、短摘要和原文链接。

以上项目完成并观察14天前，不切换到V2.0。
