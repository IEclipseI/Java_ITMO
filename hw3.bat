set prefix=C:\Workspace\ITMO_lessons\Java_hw\test_utils\java-advanced-2019\
set tests=%prefix%artifacts
set libs=%prefix%lib
set pack=ru\ifmo\rain\smirnov\implementor\
set compiled=C:\Workspace\ITMO_lessons\Java_hw\hw4_Implementor\out\production\Implementor\
set src=C:\Workspace\ITMO_lessons\Java_hw\hw4_Implementor\src\%pack%
set jar=%tests%\info.kgeorgiy.java.advanced.implementor.jar
javac -d %compiled% -cp %jar% %src%Implementor.java  
set classpath=%libs%;%tests%;%compiled%
java -p %classpath% -m info.kgeorgiy.java.advanced.implementor class ru.ifmo.rain.smirnov.implementor.Implementor