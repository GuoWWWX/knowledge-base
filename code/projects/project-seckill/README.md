# 高并发秒杀系统实战工程 (Project Seckill)

基于 Spring Boot 3 + 高并发分层防护体系的开箱即用秒杀业务工程。

---

## 1. 核心架构设计

秒杀业务具有“瞬间高并发、极少库存、高频读写冲突”的特点。本项目落地经典的三层流量漏斗拦截体系：

```
                    客户端 100+ 并发秒杀请求
                              │
                              ▼
        ┌───────────────────────────────────────────────┐
        │ 第一层：JVM 本地标记售罄内存屏障 (Memory Barrier) │ ──> 售罄后拦截 99% 穿透流量
        └──────────────────────┬────────────────────────┘
                              │ 仅放行有效抢购名额
                              ▼
        ┌───────────────────────────────────────────────┐
        │ 第二层：模拟 Redis Cluster Lua 脚本原子预扣     │ ──> 一人一单防刷 + 库存原子预扣
        └──────────────────────┬────────────────────────┘
                              │ 仅前 10 名预扣成功
                              ▼
        ┌───────────────────────────────────────────────┐
        │ 第三层：异步消息排队削峰落库 (BlockingQueue / MQ)│ ──> 异步消费，保护关系型数据库
        └──────────────────────┬────────────────────────┘
                              │ 匀速安全写入
                              ▼
        ┌───────────────────────────────────────────────┐
        │ 第四层：数据库行锁兜底 (WHERE remain_stock >= 1)│ ──> 唯一索引约束，绝对不超卖
        └───────────────────────────────────────────────┘
```

---

## 2. 核心模块一览

- **启动类**：`com.knowledge.seckill.SeckillApplication`
- **实体模型**：
  - `domain.SeckillGood`：秒杀商品实体，遵循物理平衡公式：`totalStock = remainStock + frozenStock + soldStock`。
  - `domain.SeckillOrder`：秒杀订单实体，雪花算法订单号与幂等请求号。
  - `domain.SeckillRequest` / `SeckillResult`：标准化请求 DTO 与统一响应模型。
- **业务服务**：
  - `service.SeckillService`：
    - `localOverSoldMap`：JVM 本地售罄标记屏障；
    - `executeMockRedisLuaScript`：细粒度锁保证原子一人一单与库存预扣；
    - `asyncOrderQueue` + `orderConsumerExecutor`：异步削峰下单队列与消费者线程组；
    - 数据库事务更新与联合唯一索引防重落库。
- **对外接口**：
  - `controller.SeckillController`：
    - `POST /api/seckill/doSeckill`：秒杀下单核心接口；
    - `GET /api/seckill/order`：订单状态轮询查询；
    - `GET /api/seckill/stock`：活动库存与状态对账；
    - `POST /api/seckill/init`：秒杀活动商品预热接口。
- **高并发压测主类**：
  - `SeckillConcurrencyTest`：使用 `CountDownLatch` 模拟 100 个并发线程争抢 10 件商品，验证 0 超卖与账实一致。

---

## 3. 快速启动与压测验证

### 方式 1：运行压测验证类 (包含 100 并发压测与对账)
```bash
mvn test -Dtest=SeckillConcurrencyTest -pl "20-Java后端/09-项目实战/project-seckill"
```

### 方式 2：启动 Spring Boot 服务
```bash
mvn spring-boot:run -pl "20-Java后端/09-项目实战/project-seckill"
```
启动后服务监听在 `http://localhost:8081`。

### 方式 3：调用秒杀接口
```bash
curl -X POST http://localhost:8081/api/seckill/doSeckill \
  -H "Content-Type: application/json" \
  -d '{"goodsId": 1001, "userId": 8888}'
```
