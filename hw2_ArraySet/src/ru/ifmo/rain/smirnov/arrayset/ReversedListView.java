package ru.ifmo.rain.smirnov.arrayset;

import java.util.AbstractList;
import java.util.List;

public class ReversedListView<T> extends AbstractList<T> {
    private List<T> data;
    private boolean isReversed;


    public ReversedListView(List<T> other) {
        if (other instanceof ReversedListView) {
            data = ((ReversedListView<T>) other).data;
            isReversed = !((ReversedListView) other).isReversed;
        } else {
            data = other;
            isReversed = true;
        }
    }

    @Override
    public T get(int index) {
        return isReversed ? data.get(data.size() - 1 - index) : data.get(index);
    }

    @Override
    public int size() {
        return data.size();
    }
}
