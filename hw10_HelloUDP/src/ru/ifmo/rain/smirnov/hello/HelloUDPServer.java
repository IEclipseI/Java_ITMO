package ru.ifmo.rain.smirnov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class HelloUDPServer implements HelloServer {
    private ExecutorService threadPool;
    private ExecutorService handler;
    private DatagramSocket socket;

    private final static int TIMEOUT = 1;

    static public void main(String[] args) {
        try {
            int port = Integer.parseInt(Objects.requireNonNull(args[0]));
            int threads = Integer.parseInt(Objects.requireNonNull(args[1]));
            new HelloUDPServer().start(port, threads);
        } catch (Exception e) {
            System.err.println("Arguments must be: <port> <threads count>");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(int port, int threads) {
        try {
            threadPool = Executors.newFixedThreadPool(threads);
            socket = new DatagramSocket(port);
            handler = Executors.newSingleThreadExecutor();

            handler.submit(() -> {
                try {
                    while (!Thread.interrupted() && !socket.isClosed()) {
                        DatagramPacket request = new DatagramPacket(new byte[socket.getReceiveBufferSize()], socket.getReceiveBufferSize());
                        socket.receive(request);
                        threadPool.submit(() -> process(request));
                    }
                } catch (RejectedExecutionException e) {
                    System.out.println("Server is too busy now");
                } catch (PortUnreachableException e) {
                    System.out.println("The socket is connected to a currently unreachable destination: " + e.getMessage());
                } catch (IOException e) {
                    if (!socket.isClosed())
                        System.err.println("Error: I/O error occurred: " + e.getMessage());
                }
            });
        } catch (SocketException e) {
            System.err.println("Error: the socket could not be opened, or the socket could not bind to the specified local port: ");
            System.err.println(e.getMessage());
        }
    }

    private void process(final DatagramPacket request) {
        try {
            String responseString = "Hello, " + new String(request.getData(), request.getOffset(), request.getLength(), StandardCharsets.UTF_8);
            byte[] responseBytes = responseString.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(responseBytes, responseBytes.length, request.getSocketAddress()));
        } catch (PortUnreachableException e) {
            System.out.println("The socket is connected to a currently unreachable destination. " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error: I/O error occurred." + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        handler.shutdownNow();
        threadPool.shutdownNow();
        socket.close();

        try {
            threadPool.awaitTermination(TIMEOUT, TimeUnit.MINUTES);
        } catch (InterruptedException ignored) {
        }
    }
}
