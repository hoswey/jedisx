主要是在[Jedis](https://github.com/xetorthio/jedis)的基础上增加了
* 获取最近从库
* 从库FailOver客户端感知

## 实现原理
1. 通过Sentinels获取Slaves列表
2. 启动线程定期ping从库更新RTT.
3. 监听Sentinels事件