SET sources=C:\Workspace\ITMO_lessons\Java_hw\test_utils_for_pull\java-advanced-2019\modules\info.kgeorgiy.java.advanced.concurrent\info\kgeorgiy\java\advanced\concurrent\
SET lib=%sources%\lib
SET artifacts=%sources%\artifacts


javadoc -d javaDoc -noqualifier all src\ru\ifmo\rain\smirnov\concurrent\IterativeParallelism.java %sources%\ScalarIP.java %sources%\ListIP.java