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


