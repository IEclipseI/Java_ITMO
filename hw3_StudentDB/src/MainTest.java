import ru.ifmo.rain.smirnov.student.StudentDB;

import java.util.ArrayList;
import java.util.Collections;

public class MainTest {
    public static void main(String[] args) {
        StudentDB studentDB = new StudentDB();
        studentDB.findStudentNamesByGroup(Collections.emptyList(), "");
        ArrayList<Integer> integers = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            integers.add(i);
        }
        for (int i = 0; i < integers.size(); i++) {
            integers.set(i, i*i);
            System.out.println(integers.get(i));
        }
        Object[] objects = integers.stream().filter(x -> x % 2 == 0).limit(2).toArray();

    }
}
