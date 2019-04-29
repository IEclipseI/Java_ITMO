set prefix=C:\Workspace\ITMO_lessons\Java_hw\test_utils_for_pull\java-advanced-2019\
set proj=hw10_HelloUDP
set packname=hello
set modi=server-i18n
set clazz=HelloUDPServer

set pack=ru\ifmo\rain\smirnov\%packname%\
set tests=%prefix%artifacts
set libs=%prefix%lib
set compiled=C:\Workspace\ITMO_lessons\Java_hw\%proj%\out\production\%proj%\
set src=C:\Workspace\ITMO_lessons\Java_hw\%proj%\src\%pack%
set jar=%tests%\info.kgeorgiy.java.advanced.%packname%.jar
javac -d %compiled% -cp %jar% %src%%clazz%.java
set classpath=%libs%;%tests%;%compiled%
java -p %classpath% -m info.kgeorgiy.java.advanced.%packname% %modi% ru.ifmo.rain.smirnov.%packname%.%clazz% H9