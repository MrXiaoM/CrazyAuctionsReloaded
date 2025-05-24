# CrazyAuctionsReloaded

Minecraft 全球市场插件 正在修复中  
Fork from [CrazyAuctionsPlus](https://github.com/MrXiaoM/CrazyAuctionsPlus)

> [!WARNING]
>
> 这个项目正在进行重构，可能会频繁进行破坏性变更，在重构完成之前，请勿投入使用。
>
> 预计在重构完成后，与原插件完全不兼容。但大概不会对商品模型有多少修改，正式插件将会提供迁移数据相关的帮助文档。
>

## TODO

- [x] 修正严重过时的代码，消除大部分警告
- [x] 使用 FoliaLib 处理任务调度，而不是动不动就开一条新线程
- [x] 修正数据库蹩脚的读写机制，避免出现线程不安全的异步操作
- [ ] 设计数据表，使得多服务器同时连接数据库得到支持（跨服支持）
- [ ] 引入 HikariCP 连接池，去除旧插件的重连机制（唯一没有从线程改为调度器的地方）
- [ ] 分离配置文件，使得配置起来更容易找到位置，而不是什么都塞进 `config.yml`。并且预读取配置到内存，而非需要使用时才读取。
- [ ] Not provide English configuration files any more
- [ ] 将配置文件中，键的命名规则统一为小写的串式命名

从 [PluginBase](https://github.com/MrXiaoM/PluginBase) 中引入
- [x] 物品和消息支持 Adventure+MiniMessage
- [x] 箱子菜单界面管理器
- [ ] 本地化管理器
- [ ] 操作 (`IAction`) 接口

## 赞助开发者

欢迎到 [爱发电](http://afdian.com/a/mrxiaom) 支持我的工作。
