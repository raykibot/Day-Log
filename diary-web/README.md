# 拾日 · diary-web

Vue 3 + Vite + JavaScript 前端，使用 Tailwind CSS、Vue Router 和 Axios。记录列表与新增通过 Axios 请求 Spring Boot；编辑、删除仍是当前浏览器会话中的演示操作。

## 本地运行

1. 在 MySQL 中执行相邻后端项目的 `diary/schema.sql`，创建表和 4 条示例记录。
2. 在 `diary` 目录启动 Spring Boot 服务，确认 `http://127.0.0.1:9292/api/v1/notes/noteList` 返回 JSON。
3. 在 `diary-web` 目录执行：

   ```bash
   npm install
   npm run dev
   ```

浏览器打开 `http://localhost:5173/notes`。同一局域网内也可通过终端显示的 Network 地址访问，例如 `http://192.168.1.25:5173/notes`；电脑防火墙和 Wi-Fi 设备隔离可能影响手机访问。Vite 将 `/api` 代理至本机后端 `127.0.0.1:9292`，因此手机无需直接连接 9292 端口。

## 当前页面

- `/notes`：请求 `/api/v1/notes/noteList`；选择工作或生活时发送 `category=WORK` 或 `category=LIFE`，直接渲染后端返回的记录数组。
- `/notes/:id`：读取记录详情，展示 Markdown 和附件图片；点击图片可在页面内切换、缩放和关闭；404 与连接失败有不同提示。
- `/notes/new`：上传 Markdown 与可选图片，保存到后端后返回记录列表。
- `/notes/:id/edit`：文件验证和预览可用，修改结果仅在本次页面会话中展示。
- 删除同样仅影响本次页面会话；刷新后从数据库恢复。

标题可留空，此时使用 `.md` 文件名显示。Markdown 文件须大于 0 字节且不超过 1 MB；图片仅接受 JPEG、PNG、WebP，每张不超过 20 MB。图片作为记录的独立附件展示，Markdown 经 HTML 清理后渲染。

执行 `npm run build` 生成生产构建，执行 `npx eslint .` 做静态检查。新增接口提交到 `/api/v1/notes/create`，其中 `noteDto` 为 JSON 表单项，图片使用重复的 `imageFile` 表单项。
