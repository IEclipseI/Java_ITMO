set prefix=C:\Workspace\ITMO_lessons\Java_hw\test_utils_for_pull\java-advanced-2019\
set tests=%prefix%artifacts
set libs=%prefix%lib
set pack=ru\ifmo\rain\smirnov\implementor\
set compiled=C:\Workspace\ITMO_lessons\Java_hw\hw56_JarImplementor\out\production\hw56_JarImplementor\
set src=C:\Workspace\ITMO_lessons\Java_hw\hw56_JarImplementor\src\%pack%
set jar=%tests%\info.kgeorgiy.java.advanced.implementor.jar
javac -d %compiled% -cp %jar% %src%Implementor.java  
set classpath=%libs%;%tests%;%compiled%
java  -Dfile.encoding=UTF-8 -p %classpath% -m info.kgeorgiy.java.advanced.implementor jar-class ru.ifmo.rain.smirnov.implementor.Implementor