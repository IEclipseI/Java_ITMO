package ru.ifmo.rain.smirnov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService downloadService;
    private final ExecutorService extractService;
    private final int perhost;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloadService = Executors.newFixedThreadPool(downloaders);
        this.extractService = Executors.newFixedThreadPool(extractors);
        this.perhost = perHost;
    }

    public static void main(String[] args) throws IOException {
        if (args == null || args.length < 1 || args.length > 5) {
            System.err.println("Arguments should be: url [depth [downloads [extractors [perHost]]]]");
            return;
        }
        final int defaultThreadNumber = Runtime.getRuntime().availableProcessors();
        final String url = args[0];
        final int depth = args.length >= 2 ? Integer.parseInt(args[1]) : 1;
        final int downloaders = args.length >= 3 ? Integer.parseInt(args[2]) : defaultThreadNumber;
        final int extractors = args.length >= 4 ? Integer.parseInt(args[3]) : defaultThreadNumber;
        final int perHost = args.length >= 5 ? Integer.parseInt(args[4]) : defaultThreadNumber;
        try (Crawler crawler = new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost)) {
            Result result = crawler.download(url, depth);
            System.out.println("Finished: ");
            for (String link : result.getDownloaded()) {
                System.out.println(link);
            }
            System.out.println("Errors:");
            for (Map.Entry<String, IOException> entry : result.getErrors().entrySet()) {
                System.out.println(entry.getValue() + " " + entry.getKey());
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public Result download(String url, int depth) {
        final Map<String, Integer> hosts = new HashMap<>();
        final Set<String> visited = ConcurrentHashMap.newKeySet();
        final Map<String, IOException> exceptions = new ConcurrentHashMap<>();

        Phaser phaser = new Phaser(1);
        downloadPage(url, depth, hosts, visited, exceptions, phaser);
        phaser.arriveAndAwaitAdvance();

        visited.removeAll(exceptions.keySet());
        return new Result(new ArrayList<>(visited), exceptions);
    }

    private void downloadPage(final String url, final int depth, final Map<String, Integer> hosts,
                              final Set<String> visited, final Map<String, IOException> exceptions, final Phaser phaser) {
        phaser.register();
        downloadService.execute(() -> {
            try {
                String host = URLUtils.getHost(url);
                try {
                    if (!visited.add(url))
                        return;
                    synchronized (hosts) {
                        if (hosts.containsKey(host)) {
                            try {
                                while (hosts.get(host) == perhost) {
                                    hosts.wait();
                                }
                            } catch (InterruptedException ignored) {
                            }
                            hosts.put(host, hosts.get(host) + 1);
                        } else {
                            hosts.put(host, 1);
                        }
                    }
                    final Document document = downloader.download(url);
                    synchronized (hosts) {
                        hosts.put(host, hosts.get(host) - 1);
                        hosts.notifyAll();
                    }
                    if (depth > 1) {
                        phaser.register();
                        extractService.execute(() -> {
                            try {
                                document.extractLinks()
                                        .forEach(link -> downloadPage(link, depth - 1, hosts, visited, exceptions, phaser));
                            } catch (IOException e) {
                                exceptions.put(url, e);
                            } finally {
                                phaser.arrive();
                            }
                        });
                    }
                } catch (IOException e) {
                    exceptions.put(url, e);
                    synchronized (hosts) {
                        hosts.put(host, hosts.get(host) - 1);
                        hosts.notifyAll();
                    }
                }
            } catch (MalformedURLException e) {
                visited.add(url);
                exceptions.put(url, e);
            } finally {
                phaser.arrive();
            }
        });
    }

    @Override
    public void close() {
        downloadService.shutdown();
        extractService.shutdown();
    }
}
