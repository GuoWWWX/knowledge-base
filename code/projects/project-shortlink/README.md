# 百亿级高性能短链服务实战工程 (Project ShortLink)

基于 Spring Boot 3 + Google Guava + MurmurHash3 + Base62 编码的高可用高性能短链中台实战工程。

---

## 1. 核心架构设计

在营销短信与社交分享场景中，长链接转化为 6 位短码（如 `http://t.cn/3c67GZ`），面临“哈希碰撞、缓存穿透、统计与跳转低延迟”三大挑战：

```
                      长链接输入 (originUrl)
                               │
                               ▼
        ┌───────────────────────────────────────────────┐
        │ 步骤 1：长链反查幂等索引 (longToShortMap)        │ ──> 命中则直接返回已有短码
        └──────────────────────┬────────────────────────┘
                               │ 未命中
                               ▼
        ┌───────────────────────────────────────────────┐
        │ 步骤 2：MurmurHash3 (32位无符号整数) + Base62   │ ──> 极速散列转换为 6 位字符
        └──────────────────────┬────────────────────────┘
                               │
                               ▼
        ┌───────────────────────────────────────────────┐
        │ 步骤 3：Guava 布隆过滤器防碰撞检测与加盐重散列 │ ──> 确保生成的短码全局唯一
        └──────────────────────┬────────────────────────┘
                               │
                               ▼
        ┌───────────────────────────────────────────────┐
        │ 步骤 4：短码注册写入布隆过滤器与双向高速映射   │ ──> shortToLong & longToShort
        └───────────────────────────────────────────────┘

                      客户端点击短链 (GET /{shortCode})
                               │
                               ▼
        ┌───────────────────────────────────────────────┐
        │ 第一道防线：布隆过滤器前置拦截非法探测         │ ──> 判定不存在直接 404，阻断穿透
        └──────────────────────┬────────────────────────┘
                               │ 判定存在
                               ▼
        ┌───────────────────────────────────────────────┐
        │ 第二道防线：本地高速缓存获取原始长链接         │ ──> 命中后 LongAdder 累计访问 PV
        └──────────────────────┬────────────────────────┘
                               │
                               ▼
        ┌───────────────────────────────────────────────┐
        │ 响应：HTTP 302 Found 临时重定向                │ ──> Cache-Control: no-cache 审计
        └───────────────────────────────────────────────┘
```

---

## 2. 核心模块一览

- **启动类**：`com.knowledge.shortlink.ShortLinkApplication`
- **算法与编解码工具**：
  - `util.Base62Converter`：实现 10 进制与 62 进制（0-9, a-z, A-Z）双向无损互转，6 位 Base62 包含 $62^6 \approx 568$ 亿种空间组合；
  - `util.HashUtil`：基于 Guava 的 MurmurHash3 算法将长链接转为 32 位无符号整数，支持加盐哈希。
- **业务服务**：
  - `service.ShortLinkService`：
    - 集成 `BloomFilter<String>`（100 万预期容量，0.1% 误判率）；
    - 实现哈希碰撞探测与自适应加盐重散列机制；
    - 基于 `ConcurrentHashMap` 与 `LongAdder` 的无锁并发映射与 PV 访问计数。
- **对外接口**：
  - `controller.ShortLinkController`：
    - `POST /api/shortlink/create`：传入长链接，返回 6 位短链码；
    - `GET /{shortCode}`：HTTP 302 临时重定向；
    - `GET /api/shortlink/stats/{shortCode}`：查询短码点击 PV 统计。
- **快速验证闭环类**：
  - `ShortLinkQuickRunDemo`：直接运行 `main` 方法，一键体验 Base62、MurmurHash、长短链生成、幂等保障、302重定向及布隆过滤器防穿透全链路！

---

## 3. 快速启动与验证

### 方式 1：运行快速演示闭环主类
```bash
mvn test -Dtest=ShortLinkServiceTest -pl "20-Java后端/09-项目实战/project-shortlink"
```

### 方式 2：启动 Spring Boot 短链应用
```bash
mvn spring-boot:run -pl "20-Java后端/09-项目实战/project-shortlink"
```
应用启动后监听端口：`http://localhost:8082`。

### 方式 3：生成与跳转测试
```bash
# 1. 创建短链
curl -X POST http://localhost:8082/api/shortlink/create \
  -H "Content-Type: application/json" \
  -d '{"originUrl": "https://spring.io/projects/spring-boot"}'

# 返回示例:
# {"code":200,"message":"success","data":{"shortCode":"2nTgtv","shortUrl":"http://localhost:8082/2nTgtv","originUrl":"https://spring.io/projects/spring-boot"}}

# 2. 访问短链 (返回 HTTP 302 重定向)
curl -i http://localhost:8082/2nTgtv
```
