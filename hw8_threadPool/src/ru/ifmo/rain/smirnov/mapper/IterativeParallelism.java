package ru.ifmo.rain.smirnov.mapper;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {
    private final ParallelMapper mapper;

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    public  IterativeParallelism() {
        mapper = null;
    }


    private <T, R> R parallelWork(int threads, List<? extends T> values,
                                  final Function<Stream<? extends T>, ? extends R> task,
                                  final Function<Stream<? extends R>, ? extends R> resolveResult) throws InterruptedException {
        if (threads == 0) {
            throw new IllegalArgumentException("Number of threads can not be 0");
        }

        int threadsCount = Math.max(1, Math.min(values.size(), threads));
        int partSize = values.size() / threadsCount;
        int remainder = values.size() % threadsCount;

        final List<Stream<? extends T>> partitions = new ArrayList<>();
        for (int i = 0, low = 0; i < threadsCount; i++) {
            int high = low + partSize + (remainder-- >= 1 ? 1 : 0);
            partitions.add(values.subList(low, high).stream());
            low = high;
        }

        final List<R> results;
        if (mapper != null) {
            results = mapper.map(task, partitions);
        } else {
            results = new ArrayList<>(Collections.nCopies(threadsCount, null));
            List<Thread> workers = new ArrayList<>();
            for (int i = 0; i < threadsCount; i++) {
                final int iCopy = i;
                workers.add(new Thread(() -> results.set(iCopy, task.apply(partitions.get(iCopy)))));
                workers.get(i).start();
            }

            for (int i = 0; i < threadsCount; i++) {
                workers.get(i).join();
            }
        }
        return resolveResult.apply(results.stream());
    }


    /**
     * {@inheritDoc}
     * */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return parallelWork(threads, values,
                partStream -> partStream.map(Object::toString).collect(Collectors.joining()),
                resultStream -> resultStream.collect(Collectors.joining()));
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return parallelWork(threads, values,
                partStream -> partStream.filter(predicate).collect(Collectors.toList()),
                resultStream -> resultStream.flatMap(List::stream).collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException{
        return parallelWork(threads, values,
                partStream -> partStream.map(f).collect(Collectors.toList()),
                resultStream -> resultStream.flatMap(List::stream).collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return parallelWork(threads, values,
                partStream -> partStream.max(comparator).get(),
                resultStream -> resultStream.max(comparator).get());
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
                return parallelWork(threads, values,
                partStream -> partStream.allMatch(predicate),
                resultStream -> resultStream.allMatch(t -> t));
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelWork(threads, values,
                partStream -> partStream.anyMatch(predicate),
                resultStream -> resultStream.anyMatch(t -> t));
    }
}
