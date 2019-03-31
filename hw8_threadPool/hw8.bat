set prefix=C:\Workspace\ITMO_lessons\Java_hw\test_utils_for_pull\java-advanced-2019\
set tests=%prefix%artifacts
set libs=%prefix%lib
set proj=hw8_threadPool
set pack=ru\ifmo\rain\smirnov\mapper\
set compiled=C:\Workspace\ITMO_lessons\Java_hw\%proj%\out\production\%proj%\
set src=C:\Workspace\ITMO_lessons\Java_hw\%proj%\src\%pack%
set jar=%tests%\info.kgeorgiy.java.advanced.mapper.jar;%tests%\info.kgeorgiy.java.advanced.concurrent.jar
javac -d %compiled% -cp %jar% %src%ParallelMapperImpl.java
javac -d %compiled% -cp %jar% %src%IterativeParallelism.java
set classpath=%libs%;%tests%;%compiled%
java -p %classpath% -m info.kgeorgiy.java.advanced.mapper list ru.ifmo.rain.smirnov.mapper.ParallelMapperImpl,ru.ifmo.rain.smirnov.mapper.IterativeParallelism