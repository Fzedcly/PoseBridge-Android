# PoseBridge（Android）— 低延迟 RTP H.264 推流 Demo

PoseBridge 是一个 Android 端应用，用于：
- **手机摄像头实时采集与预览**
- **H.264 编码 + RTP/UDP 推流（基于 libstreaming）**
- **自动生成 SDP 会话描述**（直接显示在 App 页面上）
- PC 端可用 **VLC / ffplay** 按 SDP 文件接收并播放画面

> 典型用途：作为“动作采集/视觉输入”的上游模块，为后续姿态重建、动作重定向、人形机器人复现提供稳定数据入口。

---

## 0. 30 秒检查清单（跑不通先看这里）
1. ✅ **手机和电脑在同一局域网**（最推荐：电脑连手机热点）  
2. ✅ App 里填的是 **电脑 IP**（例如 `192.168.43.176`），不是 `192.168.43.1`  
3. ✅ 点 Start 后 **App 能显示一大段 SDP**，并看到 `Session started OK`  
4. ✅ 电脑端是 **保存 stream.sdp → VLC 打开文件**（不要只输入 `rtp://@:5006`）  
5. ✅ 防火墙/安全软件允许 VLC 使用 UDP（或临时关闭后验证）  
6. ✅ 端口未被占用：`netstat -ano | findstr 5006`

---

## 1. 这个项目能做什么（1 分钟理解）
- **手机端**：打开 App → 输入 **接收端 PC 的 IP** → 点 **Start** → App 显示 **SDP 文本** → 推流开始
- **电脑端**：把 SDP 保存成 `stream.sdp` → 用 VLC 打开 `stream.sdp` → 看到手机摄像头画面

---

## 2. 环境准备（必须）
### 2.1 设备与软件
- **Android 真机**（推荐 Android 8+；必须真机，不建议模拟器）
- **电脑（Windows/macOS/Linux）**
- **Android Studio**（建议较新版本：Giraffe/Hedgehog/Koala 均可）
- **VLC**（电脑端播放）或 **FFmpeg（ffplay）**（可选，但排障很好用）

### 2.2 网络要求（强烈建议照做，少踩坑）
最推荐的网络拓扑（联调成功率最高）：

✅ **手机开热点** → ✅ **电脑连接手机热点**

这样手机与电脑处于同一局域网（常见网段：`192.168.43.x`），可绕开路由器隔离、校园网禁 UDP 等坑。

---

## 3. 快速跑通（10 分钟内）
### Step 1：打开工程
1. 解压/拉取项目代码
2. Android Studio → **Open** → 选择项目根目录（含 `settings.gradle` 或 `settings.gradle.kts` 的那层）
3. 等待 **Gradle Sync** 完成（首次会下载依赖，耐心等）

### Step 2：连接真机并运行
1. 手机开启：开发者选项 → USB 调试（仅用于安装/调试）
2. USB 连接电脑（第一次运行需要）
3. Android Studio 顶部选择你的设备 → **Run（绿色三角）**

> 提示：App 安装好后，后续推流不需要一直插 USB。

### Step 3：确保手机与电脑在同一网络
#### 推荐方案：手机热点
1. 手机开启热点
2. 电脑连接该热点
3. 电脑查看 IP（Windows）：
```bash
ipconfig
```
你会看到类似：
- 电脑 IP：`192.168.43.176`
- 手机热点网关（手机自己）：`192.168.43.1`

---

## 4. 手机端开始推流（关键）
1. 打开 **PoseBridge**
2. 在输入框填写：**电脑的 IPv4 地址**（示例：`192.168.43.176`）
3. 点击 **Start**
4. 正常现象：
   - App 顶部/覆盖层会出现一大段 **SDP 文本**
   - SDP 最末尾会追加：`Session started OK`

✅ 出现 **SDP + Session started OK**，说明手机端推流链路已成功（采集→编码→发送）。

---

## 5. 电脑端接收播放（VLC 必看）
> **重要**：只输入 `rtp://@:5006` 很可能黑屏，因为 VLC 需要 SDP 才能解析 payload/编码参数。

### 方案 A：VLC（最推荐）
#### Step A1：从 App 复制 SDP
- 在 App 的 SDP 文本区域：长按 → 全选 → 复制（复制全部 `v=0 ...` 那段）

#### Step A2：电脑保存为 `stream.sdp`
1. 桌面新建文件：`stream.sdp`  
   - Windows 注意：不要变成 `stream.sdp.txt`
2. 用记事本打开 `stream.sdp` → 粘贴 SDP → 保存

#### Step A3：VLC 打开 SDP 文件
- VLC → **媒体（Media）** → **打开文件（Open File）** → 选择 `stream.sdp`

✅ 成功后：VLC 会显示手机摄像头画面。

---

### 方案 B：ffplay（更底层，排障利器）
如果电脑安装了 FFmpeg：
```bash
ffplay -protocol_whitelist file,udp,rtp -i stream.sdp
```

---

## 6. 端口说明（排障时很重要）
libstreaming 常见默认端口：
- **视频 RTP**：`5006`
- **视频 RTCP**：`5007`

> 当前我们配置为 `AUDIO_NONE`（只推视频），不涉及音频端口。

---

## 7. App UI 说明（每块是什么）
- **相机预览区**：确认采集正常
- **IP 输入框**：填 “接收端 PC 的局域网 IP”
- **Start / Stop**：推流启动/停止
- **SDP 文本区**：推流会话描述（给 VLC/ffplay 用的“说明书”）

---

## 8. 常见问题（必看，省 80% 时间）
### Q1：点 Start 卡在 “Configuring session…” 或 “Starting session…”
**原因**：常见是权限未授予 / 预览 Surface 未就绪 / 会话调用顺序错误。  
**解决办法**：
1. 确认 **摄像头权限已授予**  
   - 设置 → 应用 → PoseBridge → 权限 → 允许相机
2. 确保 **相机预览画面已经显示**，再点击 Start
3. 检查代码中的会话调用顺序是否严格如下：
   - `setDestination(ip)` → `configure()` → `onSessionConfigured()` → `start()`

---

### Q2：抛出 `ConfNotSupportedException: start failed`
**原因**：当前设备 **不支持所配置的编码参数**（分辨率/帧率/码率/编码器）。  
**排查与解决思路**：
- 保持 **只推视频**：`AUDIO_NONE`
- 降低参数以提高兼容性（例如 `320x240 @ 15fps`）
- 必要时测试 **更通用的编码器/编码参数**（用于兼容性 fallback）

---

### Q3：VLC 一直加载 / 黑屏
**原因**：接收方式错误（未使用 SDP）或 UDP 端口被拦截。  
**解决办法**：
1. 使用 SDP 接收流程：保存 `stream.sdp` → VLC **打开文件**
2. 允许 VLC 通过防火墙/安全软件（UDP 5006）
3. 检查端口占用（Windows）：
```bash
netstat -ano | findstr 5006
```

---

### Q4：App 中看不到 SDP 文本（被相机画面遮住）
**原因**：`SurfaceView` 的显示层级可能覆盖普通 UI。  
**解决办法**：
- 确保代码包含：
  - `surfaceView.setZOrderMediaOverlay(true)`
- 或将预览控件替换为 `TextureView`（可选优化）

---

### Q5：App 里到底该填哪个 IP？
- 手机热点网关（手机自身）通常是：`192.168.43.1`
- **电脑 IP** 通常是：`192.168.43.xxx`（例如 `192.168.43.176`）

✅ App 必须填写 **电脑 IP**，不要填 `.1`。

---

### Q6：推流时需要一直插着 USB 吗？
不需要。USB 仅用于：
- 安装 App
- 调试 / Logcat

一旦 Start 后显示 `Session started OK`，即可拔掉 USB。

---

## 9. 代码结构（快速上手）
主逻辑位置：
- `app/src/main/java/.../MainActivity.kt`

核心流程：
1. 从 UI 读取接收端 IP
2. 使用 `SessionBuilder` 构建 `Session`（H264 + `AUDIO_NONE`）
3. `session.setDestination(ip)`
4. `session.configure()`
5. 在 `onSessionConfigured()` 中：
   - `tvSdp.text = session.sessionDescription`
   - `session.start()`
6. 停止推流：
   - `session.stop()`

布局文件：
- `app/src/main/res/layout/activity_main.xml`
  - `FrameLayout`：底层相机预览，上层 SDP 覆盖；底部为控制区域

---

## 10. 迭代记录（已解决的问题）
- ✅ 通过强制正确流程修复启动卡死：
  - `configure → onSessionConfigured → start`
- ✅ 通过“必须用 SDP”修复 VLC 黑屏
- ✅ 修复 SDP 文本被预览遮挡（`SurfaceView` overlay 问题）
- ✅ 修复权限异步导致的时序问题（权限 gating + Surface 就绪检查）
- ✅ 标准化多网络/IP 排障 SOP（`ipconfig`、`ping`、端口检查）

---

## 11. 附录：报告截图清单（建议顺序）
1. App 主界面（预览 + IP 输入 + Start/Stop）
2. Start 后的 SDP 文本（包含 `m=video 5006`、`a=rtpmap:96 H264/90000` 等）
3. 电脑端 `stream.sdp` 文件内容
4. VLC 通过 `stream.sdp` 播放画面
5. （可选）错误案例截图：权限拒绝 / 端口被拦 / 网络不可达

---

## 12. 参考（文档引用）
- libstreaming（软件）
- RFC 3550（RTP）
- RFC 4566 / RFC 8866（SDP）
- RFC 3984（H.264 over RTP）

---

## 13. 备注
本项目为课程/原型工程，优化目标：
- “端到端链路能跑通”
- “接收端易联调、易排障”

未来可扩展方向：
- 增加 pose/keypoints 数据通道
- 端侧预处理以降低带宽
- 在 UI 中展示延迟/丢包/码率等统计信息
