# Issue 25 Design QA：歌谱详情页融入 Rhythm 生态

- 日期：2026-09-09
- 分支：`feature/issue25-score-detail-rhythm-ui`
- 设备：Pixel 9 AVD，Android 16，1080 × 2424，420 dpi
- 结论：`final result: passed`

## 视觉真值与实现截图

Rhythm 视觉真值来自同一应用、同一设备状态下的专辑详情页：

- `/Users/zhangliandeng/code/personal-dev/个人/8-18 安卓播放器开发/sub/issue 25 歌谱详情页融入Rhythm生态优化/artifacts/02-rhythm-album-reference.png`

实现截图：

- 浅色详情：`.../artifacts/04-after-score-detail-light.png`
- 浅色设置：`.../artifacts/05-after-score-settings-light.png`
- 深色详情：`.../artifacts/06-after-score-detail-dark.png`
- 深色设置：`.../artifacts/07-after-score-settings-dark.png`
- 1.3 倍字体：`.../artifacts/08-after-font-scale-1.3x.png`

源图与实现图均为 1080 × 2424；并排对比图为 2160 × 2424，无需密度归一化。原生 Android 页面不使用 CSS viewport，设备像素、系统字体比例和状态栏条件均保持一致。

## 对比状态

- 浅色模式：歌谱详情默认态、设置 Bottom Sheet 展开态。
- 深色模式：Rhythm 深色 chrome + 白色谱纸，设置 Bottom Sheet 展开态。
- 字体缩放：1.3 倍，标题和状态安全截断，无按钮覆盖。
- 完整视图对比：`.../artifacts/09-reference-and-final-comparison.png`
- 设置局部前后对比：`.../artifacts/10-settings-before-after-comparison.png`
- 深色修复前后对比：`.../artifacts/11-dark-before-after-comparison.png`

## 忠实度检查

| 检查面 | 结果 |
| --- | --- |
| 字体层级 | 标题改为强层级粗体；修订号和播放状态使用主题主色辅助层级，与 Rhythm 专辑详情一致。 |
| 间距与布局 | 移除 Scaffold 重复顶部 inset；操作区采用 48dp 圆形按钮并保留谱面主体面积；1.3 倍字体无重叠。 |
| 色彩与 token | chrome、主操作、tonal 操作、容器和圆角均使用现有 Material 3 / Rhythm token，没有新建平行配色。 |
| 图标与资产 | 全部复用 `RhythmIcons`；MusicXML 仍由 alphaTab 渲染，没有占位图或伪造资产。 |
| 文案与内容 | 标题、修订、播放状态、版本导航信息保留；新增英文、简体中文、繁体中文修订文案。 |

## 修正历史

1. 初始 P1：顶部是扁平裸图标与单行标题，和 Rhythm 专辑详情的圆形 tonal 操作、标题层级不一致。已改为圆形操作、两级标题/状态，并消除重复安全区。
2. 初始 P1：设置项为无分组的长列表，且小屏不可可靠滚动。已改为可滚动 Bottom Sheet、标题区和圆角高层容器分组。
3. 初始 P0/P1：深色模式下 alphaTab 透明画布产生黑底黑色音符/歌词，核心内容不可读。已固定谱纸为白色、谱面墨色为黑色，同时保留深色 Rhythm chrome。
4. 最终复核：最新修订态曾错误把“旧版”显示为选中。已按当前索引纠正选中语义；标题装饰齿轮也改为无点击语义的 Surface。
5. 1.3 倍字体：状态文本在极限宽度下省略，操作按钮仍完整可用，判定可接受。

## 交互与稳定性

通过 ADB/UI Automator 验证播放、暂停、停止、打开设置和设置滚动；语义树可识别 `Play score`、`Pause`、`Stop`、`Score settings`。Logcat 未发现 Fatal Exception。原生页面不适用浏览器 Console。截图和语义检查不能替代 TalkBack、2.0 倍字体及 iQOO 真机完整无障碍验收。

