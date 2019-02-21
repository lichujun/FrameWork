# FrameWork

#### 项目介绍
- 1、实现IOC
- 2、实现AOP
- 3、实现基于Netty的Web服务器
- 4、实现基于Vert.x的Web服务器
#### 目前进展
- 1、实现IOC注入，通过@Component、@Controller、@Service和@Repository注解标记为bean，通过@Resource注解注入bean。
- 2、实现AOP，通过@Aspect、@After、@Before注解进行方法织入。
- 3、实现基于Netty的Web服务器，分为boss线程组和work线程组，boss负责IO，work负责业务。
- 4、实现基于Vert.x的Web服务器，重点为三大模块：event-loop、work-verticle和event-bus。event-loop负责IO，work-verticle负责业务，event-bus负责event-loop和work-verticle之间的通信。
- 注：如何使用可参考demo模块。实现事件统一捕捉处理模块，统一日志和响应的traceID，以及配置文件注解@Configuration读取。