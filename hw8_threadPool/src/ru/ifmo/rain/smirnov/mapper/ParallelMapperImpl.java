package ru.ifmo.rain.smirnov.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> threadPool;
    private final Queue<Runnable> tasks;

    public ParallelMapperImpl(int threadsCount) {
        if (threadsCount <= 0) {
            throw new IllegalArgumentException("Threads count cannot be less than 1");
        }
        threadPool = new ArrayList<>();
        tasks = new ArrayDeque<>();
        for (int i = 0; i < threadsCount; i++) {
            final Thread tmp = new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        Runnable task;
                        synchronized (tasks) {
                            while (tasks.isEmpty()) {
                                tasks.wait();
                            }
                            task = tasks.poll();
                            tasks.notifyAll();
                        }
                        task.run();
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    Thread.currentThread().interrupt();
                }
            });
            threadPool.add(tmp);
            tmp.start();
        }
    }

    private void addTask(final Runnable task) throws InterruptedException {
        synchronized (tasks) {
            tasks.add(task);
            tasks.notifyAll();
        }
    }

    private class ResultStorage<R> {
        private final List<R> res;
        private int cnt;

        ResultStorage(final int size) {
            res = new ArrayList<>(Collections.nCopies(size, null));
            cnt = 0;
        }

        void set(final int pos, R data) {
            res.set(pos, data);
            synchronized (this) {
                if (++cnt == res.size()) {
                    notify();
                }
            }
        }

        synchronized List<R> getRes() throws InterruptedException {
            while (cnt < res.size()) {
                wait();
            }
            return res;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        ResultStorage<R> result = new ResultStorage<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            final int ind = i;
            addTask(() -> result.set(ind, f.apply(args.get(ind))));
        }
        return result.getRes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        threadPool.forEach(Thread::interrupt);
        for (Thread thread : threadPool) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}
