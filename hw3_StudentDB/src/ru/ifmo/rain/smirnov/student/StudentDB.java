package ru.ifmo.rain.smirnov.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;
import static java.util.Map.Entry;


public class StudentDB implements StudentGroupQuery {

    private final static Comparator<Student> COMP_STUDENT = comparing(Student::getLastName).
            thenComparing(Student::getFirstName).
            thenComparing(Student::compareTo);

    private final static Comparator<Group> GROUP_COMPARATOR =
            comparingInt((Group x) -> -x.getStudents().size()).thenComparing(Group::getName);

    private List<String> mapListToList(List<Student> list, Function<Student, String> func) {
        return collectToList(list.stream().map(func));
    }

    private Stream<Student> filteredByStringCollectionStream(Collection<Student> collection,
                                                             Function<Student, String> func,
                                                             String s) {
        return collection.stream().filter(x -> func.apply(x).equals(s));
    }

    private List<Student> filterCollectionByStringThenSortByName(Collection<Student> collection,
                                                                 Function<Student, String> func,
                                                                 String s) {
        return sortStudentsByName(filterCollectionByString(collection, func, s, toList()));
    }

    private <R> R filterCollectionByString(Collection<Student> collection,
                                           Function<Student, String> func,
                                           String s,
                                           Collector<Student, ?, R> ctr) {
        return filteredByStringCollectionStream(collection, func, s).collect(ctr);
    }

    private <T, C extends Collection<T>> C collectTo(Stream<T> stream, Supplier<C> supplier) {
        return stream.collect(toCollection(supplier));
    }

    private <T> List<T> collectToList(Stream<T> stream) {
        return collectTo(stream, ArrayList::new);
    }

    @Override
    public List<String> getFirstNames(List<Student> list) {
        return mapListToList(list, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> list) {
        return mapListToList(list, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> list) {
        return mapListToList(list, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> list) {
        return mapListToList(list, student -> student.getFirstName() + " " + student.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> list) {
        return collectTo(list.stream().map(Student::getFirstName), TreeSet::new);
    }

    @Override
    public String getMinStudentFirstName(List<Student> list) {
        return list.stream().min(Student::compareTo).map(Student::getFirstName).orElse("");
    }

    private Stream<Student> getSortedStream(Collection<Student> collection, Comparator<Student> comp) {
        return collection.stream().sorted(comp);
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> collection) {
        return collectToList(getSortedStream(collection, Student::compareTo));
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> collection) {
        return collectToList(getSortedStream(collection, COMP_STUDENT));
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> collection, String s) {
        return filterCollectionByStringThenSortByName(collection, Student::getFirstName, s);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> collection, String s) {
        return filterCollectionByStringThenSortByName(collection, Student::getLastName, s);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> collection, String s) {
        return filterCollectionByStringThenSortByName(collection, Student::getGroup, s);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> collection, String s) {
        return filterCollectionByString(collection, Student::getGroup, s,
                toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }

    private Stream<Entry<String, List<Student>>> getGroupStream(Collection<Student> collection) {
        return collection.stream().collect(groupingBy(Student::getGroup, TreeMap::new, toList()))
                .entrySet().stream();
    }

    private Stream<Group> getSortedGroupsWithSortedStudentsStream(Collection<Student> collection,
                                                                  Function<Collection<Student>, List<Student>> studComp,
                                                                  Comparator<Group> groupComp) {
        return getGroupStream(collection)
                .map(x -> new Group(x.getKey(), studComp.apply(x.getValue())))
                .sorted(groupComp);
    }

    private <R> List<Group> getSortedGroupsWithSortedStudents(Collection<Student> collection,
                                                              Function<Collection<Student>, List<Student>> studComp,
                                                              Comparator<Group> groupComp) {
        return collectToList(getSortedGroupsWithSortedStudentsStream(collection, studComp, groupComp));
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> collection) {
        return getSortedGroupsWithSortedStudents(collection, this::sortStudentsByName, comparing(Group::getName));
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> collection) {
        return getSortedGroupsWithSortedStudents(collection, this::sortStudentsById, comparing(Group::getName));
    }

    @Override
    public String getLargestGroup(Collection<Student> collection) {
        return getSortedGroupsWithSortedStudentsStream(collection, this::sortStudentsById,
                GROUP_COMPARATOR)
                .map(Group::getName).findFirst().orElse("");
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> collection) {
        return getGroupStream(collection)
                .max(comparingInt(
                        (Entry<String, List<Student>> group) -> getDistinctFirstNames(group.getValue()).size())
                        .thenComparing(Entry::getKey, Collections.reverseOrder(String::compareTo)))
                .map(Entry::getKey).orElse("");
    }
}
