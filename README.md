# 医药雷达 Codex开发规格包

本目录是一套可以直接复制到新Git仓库根目录的开发输入文件。

## 文件说明

```text
AGENTS.md
README.md
docs/
  CURRENT_PHASE.md
  ARCHITECTURE_AND_MIGRATION.md
  PRD_V1_PERSONAL_STATIC.md
  PRD_V2_PRIVATE_SELF_HOSTED.md
  PRD_V3_PUBLIC_CLOUD.md
  CODEX_EXECUTION_GUIDE.md
  REFERENCES.md
```

### 三个产品版本

- V1.0：个人静态验证版，不购买和维护云服务器，使用GitHub Pages和GitHub Actions；
- V2.0：私有自托管版，自有设备持续运行，Spring Boot + MySQL + Docker，通过Tailscale供少量朋友同事访问；
- V3.0：公共云生产版，所有人可访问，增加境内云、公网安全、监控、账号和合规门禁。

## 开始开发

1. 新建空Git仓库；
2. 把本目录全部文件复制到仓库根目录；
3. 确认`docs/CURRENT_PHASE.md`仍为V1.0；
4. 从仓库根目录启动Codex；
5. 将`docs/CODEX_EXECUTION_GUIDE.md`中的“启动提示词”发送给Codex；
6. 审核计划后，按V1的Epic逐个执行；
7. 每个Epic完成后查看Git差异和测试结果，再进入下一个Epic。

Codex会自动读取仓库中的`AGENTS.md`作为项目级持久指令。仍应在每个新任务中指明目标PRD和需求ID，确保范围清楚。

## 重要原则

- 不要让Codex一次完成三个版本；
- V1先证明内容和使用价值；
- V2先证明私有多用户和自托管稳定性；
- V3必须先完成合规与上线评审；
- 每次只给Codex一个可测试、可回滚的工作单元；
- 所有来源和生成内容都必须可追溯；
- 不提交密钥，不保存未授权全文，不绕过网络或付费限制。

