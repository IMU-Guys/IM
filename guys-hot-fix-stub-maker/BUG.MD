# 遇到的BUG

* resources目录下需要创建 **META-INF/gradle-plugin/plugin-name.properties**。
* 在注入前将需要把用到的类和**Android**相关的类加入到 **ClassPool**中。否则会导致找不到Context之类的错误。
* 补丁方法返回值为Object类型，在原函数中返回前需要强制类型转换，否则会导致 `Cannot constrain type type new Object{ } ; value Int;`类似的问题
* 有时可以成功插桩，有时不行，原因应该是IO操作的问题，正确的方式是读取JarInput的内容，处理完之后，输出到dest文件中，不应该在创建了dest文件后又进行替换操作

`运行前需要先执行 guys-hot-fix-stub-maker的uploadArchives`