package ru.ifmo.rain.smirnov.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> data;
    private final Comparator<? super T> comparator;

    public ArraySet() {
        data = Collections.emptyList();
        comparator = null;
    }

    public ArraySet(Collection<? extends T> other) {
        this(other, null);
    }

    public ArraySet(Collection<? extends T> other, Comparator<? super T> comp) {
        comparator = comp;
        Set<T> tmp = new TreeSet<>(comparator);
        tmp.addAll(other);
        data = new ArrayList<>(tmp);
    }

    private ArraySet(List<T> other, Comparator<? super T> comp) {
            data = other;
            comparator = comp;
    }

    private ArraySet(ReversedListView<T> list, Comparator<? super T> comp) {
        data = list;
        comparator = comp;
    }

    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(data, (T) o, comparator) >= 0;
    }

    @Override
    public T lower(T t) {
        return getItem(lowerIndex(t));
    }

    @Override
    public T floor(T t) {
        return getItem(floorIndex(t));
    }

    @Override
    public T ceiling(T t) {
        return getItem(ceilingIndex(t));
    }

    @Override
    public T higher(T t) {
        return getItem(higherIndex(t));
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(data).iterator();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new ReversedListView<>(data), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        int from = fromInclusive ? ceilingIndex(fromElement) : higherIndex(fromElement);
        int to = toInclusive ? floorIndex(toElement) : lowerIndex(toElement);
        if (from > to || from < 0 || to >= data.size())
            return new ArraySet<>(Collections.emptyNavigableSet(), comparator);
        return new ArraySet<>(data.subList(from, to + 1), comparator);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (size() == 0)
            return emptySet();
        return subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (size() == 0)
            return emptySet();
        return subSet(fromElement, inclusive, last(), true);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        if (data.isEmpty())
            throw new NoSuchElementException();
        return data.get(0);
    }

    @Override
    public T last() {
        if (data.isEmpty())
            throw new NoSuchElementException();
        return data.get(data.size() - 1);
    }

    private int getItemIndex(T t, int wasFound, int wasNotFound) {
        int pos = Collections.binarySearch(data, t, comparator);
        if (pos < 0) {
            pos = -pos - 1;
            return pos + wasNotFound;
        }
        return pos + wasFound;
    }

    private boolean isCorrectIndex(int ind) {
        return 0 <= ind && ind < data.size();
    }

    private T getItem(int ind) {
        return isCorrectIndex(ind) ? data.get(ind) : null;
    }

    private int lowerIndex(T t) {
        return getItemIndex(t, -1, -1);
    }

    private int floorIndex(T t) {
        return getItemIndex(t, 0, -1);
    }

    private int ceilingIndex(T t) {
        return getItemIndex(t, 0, 0);
    }

    private int higherIndex(T t) {
        return getItemIndex(t, 1, 0);
    }

    private NavigableSet<T> emptySet() {
        return new ArraySet<>(Collections.emptyNavigableSet(), comparator);
    }

    @Override
    public int size() {
        return data.size();
    }
}