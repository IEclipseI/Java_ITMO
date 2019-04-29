package ru.ifmo.rain.smirnov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPClient implements HelloClient {
    private static int TIMEOUT = 500; // a half of second

    public static void main(String[] args) {
        try {
            String host, prefix;
            int port, threadsNum, requestsNum;
            host = args[0];
            port = Integer.parseInt(Objects.requireNonNull(args[1]));
            prefix = args[2];
            threadsNum = Integer.parseInt(Objects.requireNonNull(args[3]));
            requestsNum = Integer.parseInt(Objects.requireNonNull(args[4]));
            new HelloUDPClient().run(host, port, prefix, threadsNum, requestsNum);
        } catch (Exception e) {
            System.err.println("Arguments must be: <host> <port> <prefix> <threads count> <requests count per thread>");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        ExecutorService threadPool = Executors.newFixedThreadPool(threads);
        final InetSocketAddress socketAddress = new InetSocketAddress(host, port);
        for (int i = 0; i < threads; i++) {
            final int threadNum = i;
            threadPool.submit(() -> {
                try (DatagramSocket datagramSocket = new DatagramSocket()) {
                    for (int reqNum = 0; reqNum < requests; reqNum++) {
                        DatagramPacket response = new DatagramPacket(new byte[datagramSocket.getReceiveBufferSize()],
                                datagramSocket.getReceiveBufferSize());

                        String requestString = prefix + threadNum + "_" + reqNum;
                        byte[] requestBytes = requestString.getBytes(StandardCharsets.UTF_8);
                        DatagramPacket request = new DatagramPacket(requestBytes, requestBytes.length, socketAddress);

                        datagramSocket.setSoTimeout(TIMEOUT);
                        String responseString = "";
                        do {
                            try {
                                datagramSocket.send(request);
                                System.out.println("Request sent: " + requestString);
                                datagramSocket.receive(response);

                                responseString = new String(response.getData(), response.getOffset(), response.getLength(), StandardCharsets.UTF_8);
                            } catch (SocketTimeoutException e) {
                                System.out.println("Timeout for request is out: " + requestString);
                            } catch (PortUnreachableException e) {
                                System.out.println("Error: the socket is connected to a currently unreachable destination. " + e.getMessage());
                            } catch (IOException e) {
                                System.out.println("Error: I/O error occurs. " + e.getMessage());
                            }
                        } while (!isGoodResponse(requestString, responseString));
                        System.out.println("Response received: " + responseString);
                    }
                } catch (SocketException e) {
                    System.err.println("Error: the socket could not be opened, or the socket could not bind to the specified local port: ");
                    System.err.println(e.getMessage());
                }
            });
        }
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(requests * threads * TIMEOUT * 10, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    private boolean isGoodResponse(final String requestString, final String responseString) {
        return ("Hello, " + requestString).equals(responseString);
    }
}
