# FrameWork

#### 项目介绍
- 1、实现IOC
- 2、实现Http服务器

#### 目前进展
- 1、实现IOC注入，通过@Component、@Controller、@Service和@Repository
注解标记为bean，通过@Resource注解注入bean。以上注解均可以注入value指定bean。
@Resource的作用域为Field和Parameter，可以用@Resource注解指定bean实现接口。
可以参照ioc模块下test包使用注解进行依赖注入，测试类IocApplication，建议使用
构造函数注入参数的方式进行依赖注入。
- 2.实现Http服务器，实现了基于tomcat和netty的Http服务器，自定义了一些常见
注解，如何使用可参考demo模块。
