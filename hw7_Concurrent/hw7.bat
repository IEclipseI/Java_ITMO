set prefix=C:\Workspace\ITMO_lessons\Java_hw\test_utils_for_pull\java-advanced-2019\
set tests=%prefix%artifacts
set libs=%prefix%lib
set pack=ru\ifmo\rain\smirnov\concurrent\
set compiled=C:\Workspace\ITMO_lessons\Java_hw\hw7_Concurrent\out\production\hw7_Concurrent\
set src=C:\Workspace\ITMO_lessons\Java_hw\hw7_Concurrent\src\%pack%
set jar=%tests%\info.kgeorgiy.java.advanced.concurrent.jar
javac -d %compiled% -cp %jar% %src%IterativeParallelism.java
set classpath=%libs%;%tests%;%compiled%
java -p %classpath% -m info.kgeorgiy.java.advanced.concurrent scalar ru.ifmo.rain.smirnov.concurrent.IterativeParallelism