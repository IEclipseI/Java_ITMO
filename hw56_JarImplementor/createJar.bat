SET proj=C:\Workspace\ITMO_lessons\Java_hw\hw56_JarImplementor
SET prefix=C:\Workspace\ITMO_lessons\Java_hw\test_utils_for_pull\java-advanced-2019
SET lib=%prefix%\lib
SET test=%prefix%\artifacts\info.kgeorgiy.java.advanced.implementor.jar
SET out=out\production\artifacts
SET compiled=%proj%\out\production\hw56_JarImplementor
SET man=%proj%\src\Manifest.txt
SET dep=info\kgeorgiy\java\advanced\implementor\

cd %proj%
javac -d %out% -cp %lib%;%test%; src\ru\ifmo\rain\smirnov\implementor\Implementor.java

cd %out%
jar xf %test% %dep%Impler.class %dep%JarImpler.class %dep%ImplerException.class
jar cfm Implementor.jar %man% ru\ifmo\rain\smirnov\implementor\*.class %dep%*.class
rmdir info /s /q
cd %proj%