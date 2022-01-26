#### lucene 索引 ####
![pic](https://pic1.zhimg.com/80/v2-b601cbe28ef7c822b393451cf2347e9c_720w.jpg)
Lucene利用FST进行压缩

>+ 压缩节省磁盘空间
![DD](https://pic4.zhimg.com/80/v2-a3ee78a1dbc82f9d660adcf940b26687_720w.jpg)
>+ 快速求交集
```text
option：  integer数组
option2： bitmap
option3:  roaring bitmaps (临界值, 小于N用Integer, 反之bitmap.
```
![dd](https://pic3.zhimg.com/80/v2-1c9f5518671ace1cae24f819cd8c049e_720w.jpg)

#### 文件
![文件](https://img-blog.csdn.net/20170105081126996?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvbmpwanNvZnRkZXY=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)
![索引结构](https://img-blog.csdn.net/20170103134219007?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvcm9uYWxvZA==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)


>+ [lucene源码分析](https://blog.csdn.net/qqqq0199181/article/details/89178419)
>+ [lucene源码分析2](https://blog.csdn.net/liweisnake/category_1607677.html)
>+ [lucene简介和原理](https://www.cnblogs.com/sessionbest/articles/8689030.html)
>+ [参考](https://zhuanlan.zhihu.com/p/76485252) 


#### [余玄相似度](https://www.cnblogs.com/airnew/p/9563703.html)
![COS](https://images2018.cnblogs.com/blog/110616/201808/110616-20180831064110074-435406859.png)
![三维空间余玄函数](https://images2018.cnblogs.com/blog/110616/201808/110616-20180831064147723-1586786294.jpg) 
![RESULT](https://images2018.cnblogs.com/blog/110616/201808/110616-20180831064912918-1871835271.png)


>+ [相似度方法对比](https://zhuanlan.zhihu.com/p/37104535)
>+ [onehot编码](https://blog.csdn.net/qq_15192373/article/details/89552498) 