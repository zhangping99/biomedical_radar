# V1.0 P0 验收记录

## 结论

截至 2026-09-07，V1.0 P0 的代码、契约、自动化工作流和本地回归已完成。当前状态是“可提交并进入线上/真机观察”，不是“V1全部验收完成”：GitHub Pages 首次部署、连续 7 次定时采集、两类真机安装测试和完整内容许可复核仍需外部环境或人工完成。

## 自动化验证结果

| 门禁 | 结果 | 证据范围 |
|---|---|---|
| Java 17 Maven `verify` | 通过 | `radar-domain` 3 项、`radar-collector` 4 项，共 7 项测试；包含六来源fixture、单源失败隔离、去重、评分、未来日期保护和过期日文件清理 |
| 静态数据契约 | 通过 | Article、Feed、source-health schema及SHA-256；当前发布目录共验证15个文件 |
| ESLint | 通过 | 0 warning门禁 |
| Vitest | 通过 | 4个测试文件、7项测试 |
| 生产构建 | 通过 | TypeScript检查、Vite构建、PWA manifest与Service Worker生成 |
| Playwright移动端 | 通过 | iPhone 13 WebKit与Pixel 7 Chromium共8项；覆盖加载/详情追溯、筛选/收藏、横向溢出和IndexedDB降级 |
| 真实来源试采 | 部分成功 | 50条记录；FDA、EMA、ClinicalTrials.gov、Europe PMC、Amgen成功，NMPA为`SSLHANDSHAKEEXCEPTION`；单源失败未中止导出 |

## PRD验收项

| ID | 当前状态 | 说明 |
|---|---|---|
| V1-A-001 | 待线上观察 | 工作流和单源失败隔离已实现；仍需GitHub Actions连续7次成功记录 |
| V1-A-002 | 已通过 | 当前发布JSON全部通过Schema及文件哈希校验 |
| V1-A-003 | 已通过 | 跨来源保守去重与全部来源链接保留有领域测试 |
| V1-A-004 | 待真机 | WebKit模拟回归通过；需iPhone添加主屏幕并独立启动 |
| V1-A-005 | 待真机 | Chromium模拟回归通过；需Android安装并独立启动 |
| V1-A-006 | 部分通过 | 请求失败读取IndexedDB最近缓存已通过；需真机飞行模式复核Service Worker启动 |
| V1-A-007 | 部分通过 | 中英文搜索与组合筛选测试通过；1万条目标设备性能尚未压测 |
| V1-A-008 | 部分通过 | 收藏、IndexedDB与备份导入导出自动化通过；需真机重启浏览器复核 |
| V1-A-009 | 部分通过 | 当前50条真实记录均通过必填来源/时间/链接契约；尚不足100条人工抽检样本 |
| V1-A-010 | 部分通过 | fixture最小化、构建产物和差异敏感信息扫描已纳入交付检查；来源许可仍需定期人工复核 |
| V1-A-011 | 已通过 | 数据请求失败时保留缓存并展示明确状态，Playwright已覆盖 |
| V1-A-012 | 已通过 | manifest新鲜度及来源失败提示已实现 |

## 发布前人工清单

- 在 GitHub 仓库 Settings → Pages 选择 GitHub Actions，并确认首次部署地址。
- 手动执行一次 `CI` 与 `Collect static feed`，确认GitHub托管环境中的NMPA TLS结果。
- 记录连续7次采集结果；单源失败可接受，但不得全部来源失败或发布无效JSON。
- 分别在真实iPhone和Android上验证安装、独立窗口、弱网、飞行模式、返回滚动位置与44px点击目标。
- 对六个来源的使用条款、转载边界与页面结构做人工复核；只保留元数据、短摘要和原文链接。

以上项目完成并观察14天前，不切换到V2.0。
