### [循环依赖和代理的关系](https://juejin.cn/post/6882266649509298189) ###
![1](https://images.alsritter.icu/images/2021/05/12/20210512154019.png)

> 创建过程： DefaultSingletonFactory#getSingleton
>* singleObject (1) 
>* earlySingleObject (2)   
>* singleObjectFactory (3)
>* earlyProxyObject 

>+ 1.1  addSingletonFactory --> (3) PUT, (2) REMOVE [NULL]
>+ 1.2  getSingleton --> (3) REMOVE, (2) PUT [代理替换对象]
>+ 1.3  addSingleton --> (2) REMOVE, (1) PUT 


>+ spring依赖两个核心特性： 依赖注入、面向切面

## 100+ QA

1、cglib和jdk动态代理的区别
2、切面相关： 事务的四大特性
3、bean的生命周期
4、spring设计模式总结和应用（工厂beanFactory、单例、代理-cglib|jdk、模板JpaTemplate、观察者ApplicationListener 
5、FactoryBean, mybatis的应用
6、[BeanFactory和ApplicationContext的区别](https://www.cnblogs.com/crazymakercircle/p/14465630.html)
7、bean的作用域
```
Spring框架支持以下五种bean的作用域：
 singleton : bean在每个Spring ioc 容器中只有一个实例。
 prototype：一个bean的定义可以有多个实例。
 request：每次http请求都会创建一个bean，该作用域仅在基于web的Spring ApplicationContext情形下有效。
 session：在一个HTTP Session中，一个bean定义对应一个实例。该作用域仅在基于web的Spring ApplicationContext情形下有效。
 global-session：在一个全局的HTTP Session中，一个bean定义对应一个实例。该作用域仅在基于web的Spring ApplicationContext情形下有效。
注意： 缺省的Spring bean 的作用域是Singleton。使用 prototype 作用域需要慎重的思考，因为频繁创建和销毁 bean 会带来很大的性能开销。

```
8、ThreadLocal和Pool(kryo)
9、 spring如何解决循环依赖


