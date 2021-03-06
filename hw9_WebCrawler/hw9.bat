set prefix=C:\Workspace\ITMO_lessons\Java_hw\test_utils_for_pull\java-advanced-2019\
set proj=hw9_WebCrawler
set packname=crawler
set modi=hard
set clazz=WebCrawler

set pack=ru\ifmo\rain\smirnov\%packname%\
set tests=%prefix%artifacts
set libs=%prefix%lib
set compiled=C:\Workspace\ITMO_lessons\Java_hw\%proj%\out\production\%proj%\
set src=C:\Workspace\ITMO_lessons\Java_hw\%proj%\src\%pack%
set jar=%tests%\info.kgeorgiy.java.advanced.%packname%.jar
javac -d %compiled% -cp %jar% %src%%clazz%.java
set classpath=%libs%;%tests%;%compiled%
java -p %classpath% -m info.kgeorgiy.java.advanced.%packname% %modi% ru.ifmo.rain.smirnov.%packname%.%clazz% dk