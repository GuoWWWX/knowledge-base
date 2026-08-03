---
title: 面试-JVM
tags:
  - 领域/求职
  - 领域/Java后端
  - 类型/面试
  - 主题/JVM
created: 2026-07-04
updated: 2026-07-26
status: 已完成
mastery: ⭐☆☆☆☆
next_review: 2026-07-29
---

# 面试-JVM

> [!abstract] 一句话总结
> JVM 面试的答题主线是"**结论 → 原理 → 证据**"：先给一句话结论，再讲内存/GC/类加载的机制，最后落到一次真实排查（现象 → jstat/jmap/Arthas 取证 → 定位 → 修复）。能把排查命令说出口径的人，和只会背八股的人，面试官一听就能分辨。

## 🎯 回答框架

1. **先给结论**：一句话直接回答，不绕。
2. **展开原理**：讲机制、边界、版本差异（JDK 8 vs 11 vs 17+）。
3. **结合项目**：落到一次线上问题——现象、取证命令、dump 分析、修复动作。
4. **主动补取舍**：为什么选这个收集器/参数，代价是什么。

## 面试官最想听到的 5 个意识

- **调优的前提是证据**：没有 GC 日志和前后对比数据的"调优"都是玄学。
- **先修代码再调参数**：多数"JVM 问题"其实是无界缓存、大查询、线程失控。
- **版本意识**：答案要区分 JDK 8（永久代已死、CMS 时代）和 JDK 17+（G1 默认、ZGC 可用）。
- **停顿目标是"表达"出来的**：G1 之后的思路是告诉 JVM 目标，而不是手调各代比例。
- **命令要能脱口而出**：jstat/jmap/jstack/Arthas 的具体参数说不出来，案例就不可信。

## 高频问题

> [!question]- Q1: JVM 运行时内存区域有哪些？各区域分别会抛什么 OOM？
> **30 秒版**：线程私有：程序计数器、虚拟机栈、本地方法栈；线程共享：堆、方法区（JDK 8 起由元空间实现）。堆抛 `Java heap space`；栈深度超限抛 `StackOverflowError`，创建线程失败抛 `unable to create new native thread`；元空间抛 `Metaspace`；直接内存抛 `Direct buffer memory`。
> **2 分钟版**（按"谁私有、放什么、怎么溢出"三条线讲）：
> - **程序计数器**：记录当前字节码行号，唯一不会 OOM 的区域。
> - **虚拟机栈**：每次方法调用压一个栈帧（局部变量表、操作数栈、动态链接、返回地址）。无限递归 → StackOverflowError；线程数过多耗尽本机内存 → unable to create new native thread——注意这是**操作系统内存不够而不是堆不够**，减小 -Xss 或减线程数反而能缓解。
> - **堆**：对象主战场，-Xms/-Xmx 控制，OOM 最常见来源。
> - **元空间**：JDK 8 移除永久代改用本地内存。CGLIB 动态代理、Groovy 脚本、频繁热部署会生成海量类，打爆 Metaspace。
> - **直接内存**：NIO 的 DirectByteBuffer 分配，受 -XX:MaxDirectMemorySize 限制，Netty 应用重点关注。
> **追问应对**：常追"哪个区域不会 OOM"（程序计数器）和"线程 OOM 为什么调大 -Xmx 反而更糟"（堆占得越多，留给线程栈的本地内存越少）。

> [!question]- Q2: 对象是怎么创建的？什么是逃逸分析？对象一定分配在堆上吗？
> **30 秒版**：类加载检查 → 分配内存（指针碰撞或空闲列表）→ 零值初始化 → 设置对象头 → 执行构造方法。逃逸分析是 JIT 判断对象是否逃出方法/线程，不逃逸就做标量替换、锁消除，所以对象**不一定**在堆上。
> **2 分钟版**：
> - **分配的线程安全**靠两招：CAS + 失败重试；TLAB——每个线程在 Eden 预留私有缓冲，分配无竞争，默认开启。
> - **逃逸分析**（-XX:+DoEscapeAnalysis，默认开）判定不逃逸后，实际做的是**标量替换**而非真正"栈上分配"：把对象拆成基本类型放进局部变量表，对象根本不存在了。
> - 配套优化还有**锁消除**（不逃逸对象上的 synchronized 直接去掉）和锁粗化。
> **项目结合**：高频方法里 new 临时 DTO/StringBuilder，经逃逸分析后几乎无堆分配开销，所以不必手写对象池；反而对象池长期持有引用会让对象晋升老年代，弄巧成拙。

> [!question]- Q3: GC 如何判断对象已死？为什么不用引用计数？
> **30 秒版**：可达性分析——从 GC Roots 出发走不到的对象即可回收；引用计数无法处理循环引用，主流 JVM 不用。
> **2 分钟版**：
> - **GC Roots 要能列举**：虚拟机栈/本地方法栈中的引用、方法区静态属性与常量引用、活跃线程对象、被 synchronized 持有的对象、JNI 引用。
> - **四种引用强度**：强（不回收）、软（内存不足才回收）、弱（下次 GC 必回收）、虚（仅回收通知，Cleaner 靠它释放 DirectByteBuffer 堆外内存）。
> **追问应对**：面试官常顺势追 **ThreadLocal 内存泄漏**——Entry 的 key 是弱引用会被回收，但 value 是强引用挂在线程的 ThreadLocalMap 上；线程池里线程不死，value 永远泄漏，所以用完必须 remove()。这是把 JVM 和并发串起来的高分点。

> [!question]- Q4: GC 算法有哪几种？为什么堆要分代？
> **30 秒版**：标记-清除（有碎片）、标记-复制（无碎片但费一半空间）、标记-整理（无碎片但移动贵）。分代依据是"绝大多数对象朝生夕死"：新生代存活少用复制（复制的少、便宜），老年代存活多用清除/整理。
> **2 分钟版**：
> - **新生代结构**：Eden : S0 : S1 = 8 : 1 : 1。Minor GC 流程：Eden 满触发 → 存活对象复制到 Survivor to 区 → 年龄 +1 → 达阈值（默认 15，或动态年龄判定：同龄对象总量超 Survivor 一半）晋升老年代。
> - **跨代引用**：老年代引用新生代对象时，Minor GC 不必扫全堆——卡表（Card Table）+ 写屏障记录"脏卡"，只扫脏卡区域。
> **追问应对**：
> - "什么对象直接进老年代？"——大对象、长期存活对象、动态年龄判定命中、Survivor 装不下时的担保分配。
> - "Minor / Full GC 触发条件？"——Eden 满触发 Minor；老年代不足、元空间不足、System.gc()、空间分配担保失败触发 Full。

> [!question]- Q5: CMS、G1、ZGC 各自的思路是什么？线上怎么选？
> **30 秒版**：CMS 并发标记清除但有碎片和并发失败问题，JDK 14 已移除；G1 把堆切成 Region、按收益挑着回收、可预测停顿，是 JDK 9+ 默认；ZGC 用染色指针 + 读屏障，停顿与堆大小无关（亚毫秒级）。选型看 JDK 版本、堆大小、延迟目标三个变量。
> **2 分钟版**：
> - **G1 关键机制**：堆划分约 2048 个 Region，逻辑分代物理不分代；每个 Region 用 Remembered Set 记录"谁引用了我"，从而能只回收部分 Region（Collection Set）；Mixed GC 顺带回收收益最高的老年代 Region；停顿预测模型按 -XX:MaxGCPauseMillis 目标挑选本次回收量。
> - **ZGC 关键机制**：在 64 位指针里借几位做标记（染色指针），配合读屏障让应用线程访问对象时"顺手"完成转移修正——标记和转移几乎全并发，停顿不随堆增长。
> **选型口径（项目结合）**：
> - 8G 堆、延迟 100ms 级的普通 Web 服务 → **G1**，默认即最稳。
> - 32G+ 大堆、P99 要求 10ms 内、JDK 17+ → **ZGC**。
> - 存量 JDK 8 → 大堆 G1，小堆（4G 内）ParNew+CMS 或 Parallel。
> - 主动说代价：ZGC 读屏障约损失 5~10% 吞吐、内存占用更高，不是所有服务都值得上。

> [!question]- Q6: 类加载过程和双亲委派是什么？哪些场景要打破它？
> **30 秒版**：加载 → 验证 → 准备 → 解析 → 初始化五步。双亲委派：先委托父加载器，父加载不了才自己来，保证核心类不被篡改、类全局唯一。打破场景：JDBC 等 SPI、Tomcat 应用隔离、OSGi/插件化、热部署。
> **2 分钟版**：
> - **三层加载器**：Bootstrap（核心类库）→ Platform/Ext → Application。
> - **细节分**：准备阶段静态变量赋零值，初始化阶段才执行 static 块——`static int a = 1` 在准备阶段 a=0。
> - **打破的三种典型方式**：① SPI 用线程上下文类加载器（TCCL）——DriverManager 在核心库由 Bootstrap 加载，MySQL 驱动却在应用 classpath，只能"向下"借 TCCL 反向加载；② Tomcat 的 WebAppClassLoader 先自己加载再委托，实现多应用同名类隔离；③ 自定义 ClassLoader 重写 loadClass（而非 findClass）做热替换——注意同名类被不同加载器加载是两个类，互相 cast 抛 ClassCastException。
> **追问应对**（三个异常秒答区别）：
> - `ClassNotFoundException`：显式加载时 classpath 找不到。
> - `NoClassDefFoundError`：编译期在、运行期没了（依赖冲突、static 块抛异常后二次引用）。
> - `LinkageError/ClassCastException`：多加载器加载了同名类。

> [!question]- Q7: JVM 调优参数的思路是什么？说几个你真正用过的参数。
> **30 秒版**：前提是有指标和证据（GC 日志、监控），目标是吞吐、延迟、内存三选二，大多数情况下先修代码再调参数。核心：-Xms/-Xmx 设相等、G1 用 MaxGCPauseMillis 表达延迟目标、必开 GC 日志和 OOM 自动 dump。
> **2 分钟版**（一套可直接背出的生产基线，8G 容器、JDK 17、G1）：
> ```text
> -Xms4g -Xmx4g                          # 堆固定，避免动态扩缩引发 GC
> -XX:+UseG1GC -XX:MaxGCPauseMillis=100  # 表达目标而不是手调代数比例
> -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m
> -XX:+HeapDumpOnOutOfMemoryError
> -XX:HeapDumpPath=/logs/dump.hprof
> -Xlog:gc*:file=/logs/gc.log:time,uptime:filecount=5,filesize=50m
> ```
> - 容器环境堆给容器内存的 50~70%：要给元空间、线程栈、直接内存留份额，否则堆没 OOM 容器先被 OOMKilled。
> - G1 时代不建议手动设 -Xmn / SurvivorRatio，会干扰其自适应停顿模型。
> **追问应对**："调优效果怎么量化？"——jstat/GC 日志前后对比 Young GC 频率与耗时、Full GC 次数、P99 停顿、晋升速率。主动说"没有前后对比的调优都是玄学"本身就是加分项。

> [!question]- Q8: 线上频繁 Full GC / OOM，你的排查步骤是什么？（重点题，命令必须背熟）
> **30 秒版**：先看监控和 GC 日志确认现象 → jstat 看各代占用与 GC 趋势 → jmap/Arthas 抓对象直方图和堆 dump → MAT 看支配树和 GC Roots 引用链 → 区分泄漏、缓存无界、大查询、流量突增还是参数过小。
> **2 分钟版（完整命令口径，按顺序背）**：
> ```bash
> # 1. 找进程、看整体趋势
> jps -l                        # 或 top 找 PID
> jstat -gcutil <pid> 1000 10   # 每秒一行：看 O 区水位和 FGC 增速
>                               # 关键判断：Full GC 后 O 区降不下来 → 泄漏或容量不足
> # 2. 低开销先看大对象分布
> jmap -histo:live <pid> | head -20    # 注意 :live 会触发一次 Full GC
> # 3. 抓完整 dump（大堆会卡顿，先摘流量再抓）
> jmap -dump:live,format=b,file=/tmp/heap.hprof <pid>
> # 4. 怀疑线程问题 / CPU 高
> jstack <pid> > /tmp/stack.txt        # 看 BLOCKED、死锁、线程总数
> # 5. Arthas 在线诊断（不重启不停机）
> dashboard                            # 内存、GC、线程总览
> heapdump /tmp/a.hprof
> profiler start --event alloc         # 分配火焰图定位分配热点
> watch com.xx.Service method '{params, returnObj}' -x 2
> ```
> dump 用 MAT 分析：Leak Suspects → Dominator Tree 找最大占用 → Path to GC Roots（排除弱引用）找到谁持有不放。
> **项目结合（可整段复述的案例）**：某服务每小时 Full GC 一次且 O 区回收后仍 80%+。jstat 确认老年代持续增长；jmap -histo 发现某 DTO 实例达千万级；MAT 支配树定位到一个 static HashMap 本地缓存无上限，且 key 带时间戳永不命中、只增不减。修复：换 Caffeine 设 maximumSize + expireAfterWrite，Full GC 降到每天零星一次。**把证据链讲全，比背十条理论都值钱。**

> [!question]- Q9: Young GC 频繁但 Full GC 正常，需要处理吗？
> **30 秒版**：先看代价——单次几毫秒~几十毫秒、几秒一次通常可接受；要警惕的是单次耗时上升或"过早晋升"把压力传导给老年代。方向：分配速率过高或新生代过小。
> **2 分钟版**：
> - 用 jstat -gc 估算分配速率（Eden 增长量 / 时间）。
> - 常见原因一：热点路径大量临时对象（JSON 序列化、字符串拼接、一次捞几万行）——Arthas `profiler --event alloc` 抓分配火焰图定位。
> - 常见原因二：新生代过小，Eden 频繁打满，对象没活几轮就被迫晋升——观察 Survivor 使用率和实际晋升年龄。
> - 处理顺序：先优化分配热点，再考虑加大堆/新生代。
> - 记住因果链：**Young GC 频繁不可怕，晋升速率高才可怕——它最终制造 Full GC。**
> **追问应对**："怎么确认过早晋升？"——GC 日志看晋升字节数，或 jstat 观察每次 Young GC 后 O 区增量；G1 日志关注 to-space exhausted / Evacuation Failure。

> [!question]- Q10: CPU 飙高怎么定位到具体代码？
> **30 秒版**：top 找进程 → top -Hp 找线程 → 线程号转 16 进制 → jstack 里搜 nid 看栈；或 Arthas 一条 `thread -n 3` 直接列出最忙的 3 个线程栈。
> **2 分钟版**：
> - 命令链：`top` → `top -Hp <pid>` → `printf '%x\n' <tid>` → `jstack <pid> | grep -A 30 nid=0x<hex>`。
> - 按栈的落点分三类结局：① 落在业务代码 → 死循环或热点算法（正则回溯、大 JSON 反序列化是经典案例）；② 落在 "GC task thread" 等 → 是 GC 在烧 CPU，转向内存排查（回到 Q8）；③ 大量 BLOCKED → 锁竞争，jstack 找持锁线程，或 Arthas `thread -b` 直接定位死锁。
> **项目结合**：强调 Arthas 的价值是生产环境不重启、秒级定位，`thread -n`、`watch`、`trace` 三板斧要能随口说出。

> [!question]- Q11: 强软弱虚四种引用分别什么时候用？
> **30 秒版**：强引用绝不回收；软引用内存不足才回收，适合可有可无的缓存；弱引用活不过下次 GC，用于不阻止回收的映射（WeakHashMap、ThreadLocal key）；虚引用拿不到对象，只用于回收通知、管理堆外资源。
> **2 分钟版**（各举一个 JDK 内部实例就是满分）：
> - 软引用：图片/结果缓存——但生产更推荐 Caffeine 显式上限，因为软引用回收时机不可控。
> - 弱引用：ThreadLocalMap 的 Entry key。
> - 虚引用：Cleaner 机制释放 DirectByteBuffer 的堆外内存。
> **追问应对**："为什么缓存不建议用软引用？"——软引用的回收发生在内存紧张时，恰是系统最脆弱的时刻，集中回收等于雪上加霜，且命中率不可预测、无法容量规划。

> [!question]- Q12: 老年代明明没满，为什么也会 Full GC？空间分配担保是什么？
> **30 秒版**：Minor GC 前 JVM 检查老年代最大连续可用空间是否大于历次晋升的平均值：够就放心 Minor GC，不够先 Full GC 腾地方——这就是空间分配担保（Survivor 装不下的存活对象由老年代兜底）。
> **2 分钟版**：
> - 复制算法要求有地方接收存活对象，Survivor 只占新生代 10%，装不下的部分靠老年代担保。
> - JDK 6u24 后策略固定为："老年代连续空间 > 历次晋升平均大小"就冒险 Minor GC，否则先 Full GC。
> - 这道题解释一类现象：**老年代没满却 Full GC**——担保失败（promotion failed）、元空间不足、System.gc() 是三大答案。
> **追问应对**：G1 中对应概念是 Evacuation Failure / to-space exhausted；处理思路：增大堆、调大 -XX:G1ReservePercent。

## 🧰 排查命令速查表（面试前 5 分钟扫一遍）

| 场景 | 命令 | 看什么 |
| ---- | ---- | ---- |
| GC 趋势 | `jstat -gcutil <pid> 1000` | O 区水位、FGC 增速、Full GC 后是否回落 |
| 大对象分布 | `jmap -histo:live <pid>` | 实例数/字节数 Top 类（:live 触发 Full GC） |
| 堆快照 | `jmap -dump:live,format=b,file=x.hprof <pid>` | 交给 MAT：Leak Suspects → 支配树 → GC Roots |
| 线程/锁 | `jstack <pid>` | BLOCKED、deadlock、线程总数 |
| CPU 热点 | `top -Hp` + `jstack` 搜 nid / Arthas `thread -n 3` | 最忙线程的栈落在业务还是 GC |
| 在线观测 | Arthas `dashboard` / `watch` / `trace` / `sc -d` | 不重启定位方法耗时、参数、类加载来源 |

## 🔁 追问链预演（面试官最可能的三条追问路径）

**链路 1：内存区域 → OOM → 排查命令 → dump 分析**
- "说说内存区域" → "堆 OOM 和栈溢出的区别？"
- → "线上堆 OOM 第一步做什么？"——确认有没有 HeapDumpOnOutOfMemoryError 留下的 dump，没有就 jmap 补抓 + jstat 看趋势。
- → "dump 几十 GB 怎么分析？"——MAT 命令行 ParseHeapDump 在服务器上先出报告，或先用 jmap -histo 缩小范围。
- → "怎么区分泄漏和容量不足？"——看 Full GC 后老年代能否回落：泄漏是持续上涨回不去，容量不足是能回落但整体水位高。

**链路 2：GC 分代 → G1 原理 → 停顿目标 → 效果验证**
- "为什么分代" → "G1 还分代吗？"——逻辑分代、物理不分代，Region 动态扮演 Eden/Survivor/Old。
- → "MaxGCPauseMillis 设成 10ms 会怎样？"——G1 缩小新生代、减少单次回收量来凑目标，结果 GC 更频繁、吞吐下降，跟不上分配速率时退化 Full GC，目标要设得合理。
- → "怎么证明调优有效？"——GC 日志前后对比 Young/Full 频率、P99 停顿、晋升速率。

**链路 3：双亲委派 → 打破场景 → 类冲突事故**
- "双亲委派是什么" → "JDBC 为什么要打破？"——核心库的 DriverManager 要加载应用 classpath 里的驱动，Bootstrap 够不着，用 TCCL 反向加载。
- → "Tomcat 怎么做类隔离？"——每个应用一个 WebAppClassLoader，先自加载再委托。
- → "遇到过类冲突吗？"——准备一个案例：两个依赖携带不同版本同名类导致 NoSuchMethodError，用 Arthas `sc -d 类名` 查实际加载来源和加载器，exclude 掉旧依赖解决。

## 🔗 关联笔记

- [[../20-Java后端/02-JVM/01-JVM内存区域]]
- [[../20-Java后端/02-JVM/04-垃圾收集器-G1-ZGC]]
- [[../20-Java后端/02-JVM/07-OOM问题排查]]
- [[01-面试-LLM基础]]（同一套"结论 → 原理 → 项目"答题框架）
- [[40-求职准备/_index|求职准备索引]]

## 📚 参考资料

- 《深入理解 Java 虚拟机》第 3 版（周志明）第 2、3、7、8 章
- Oracle GC Tuning Guide（对应线上 JDK 版本）
- Arthas 官方文档：thread / dashboard / heapdump / profiler / sc
