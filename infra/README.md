# V1 基础设施边界

V1 只使用 GitHub Actions 与 GitHub Pages，配置位于 `.github/workflows`。本目录不包含服务器、数据库、Docker Compose、Tailscale 或境内云资源。

## 回滚

1. 在 GitHub 仓库的 Actions 页面找到最近一次已知正常的 `Deploy GitHub Pages` 运行；
2. 将 `main` 中有问题的数据或代码提交通过普通 `git revert <commit>` 回退，不重写历史；
3. 等待 CI 通过后手动运行 `Deploy GitHub Pages`；
4. 验证首页、列表、详情、离线壳和原文链接。部署失败不会替换上一版 Pages 站点。

首次启用时，在仓库 Settings → Pages → Build and deployment 中把 Source 设为 **GitHub Actions**。
