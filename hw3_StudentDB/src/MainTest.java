import info.kgeorgiy.java.advanced.student.Student;
import ru.ifmo.rain.smirnov.student.StudentDB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainTest {
    public static void main(String[] args) {
        Student s1 = new Student(1, "qwe", "qwe3", "2");
        Student s2 = new Student(1, "qwe", "qwe3", "3");
        Student s3 = new Student(1, "qwe", "qwe3", "1");
        Student s4 = new Student(2, "qwe", "qwe", "1");
        Student s5 = new Student(3, "qwe", "qwe", "2");
        List<Student> st = new ArrayList<>();
        st.add(s1);st.add(s2);st.add(s3);st.add(s4);st.add(s5);
        String pop = new StudentDB().getMostPopularName(st);
        System.out.println(pop);
    }
}
