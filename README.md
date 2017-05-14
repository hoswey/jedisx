主要是在[Jedis](https://github.com/xetorthio/jedis)的基础上增加了
* 获取最近从库，已满足读写分离，以及跨机房就近读的需求
* FailOver客户端感知，实时更新Master Slave列表

## 实现原理
1. 通过Sentinels获取Slaves列表
2. 启动线程定期ping从库更新RTT.
3. 监听Sentinels事件

## 如何使用

```java
public class Test {

  public static void main(String[] args) throws Exception{
    
    Set<String> sentinels = new HashSet<>(Arrays.asList("172.26.40.16:26379",
        "172.26.40.16:26380", "172.26.40.16:26381"));
    JedisxSentinelPool cluster = new JedisxSentinelPool("mymaster", sentinels);

    String key = "foo";
    String value = "bar";
    
    //获取主库
    try (Jedis jedis = cluster.getResource()) {
      jedis.set(key, value);
    }

    //睡眠一定时间保证从库能及时同步
    
    Thread.sleep(100);
    //获取最近的一个从库，假如没有从库，则获取主库
    try (Jedis jedis = cluster.getNearestResource()) {
      String valueInSlave = jedis.get(key);
      assert valueInSlave.equals(value);
    }
  }
}
  ```