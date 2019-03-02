set prefix=C:\Workspace\ITMO_lessons\Java_hw\test_utils\java-advanced-2019\
set tests=%prefix%artifacts
set libs=%prefix%lib
set pack=ru\ifmo\rain\smirnov\student\
set compiled=C:\Workspace\ITMO_lessons\Java_hw\hw3_StudentDB\out\production\StudentDB\
set src=C:\Workspace\ITMO_lessons\Java_hw\hw3_StudentDB\src\%pack%
set student_jar=%tests%\info.kgeorgiy.java.advanced.student.jar
javac -d %compiled% -cp %student_jar% %src%StudentDB.java  
set classpath=%libs%;%tests%;%compiled%
java -p %classpath% -m info.kgeorgiy.java.advanced.student StudentGroupQuery ru.ifmo.rain.smirnov.student.StudentDB