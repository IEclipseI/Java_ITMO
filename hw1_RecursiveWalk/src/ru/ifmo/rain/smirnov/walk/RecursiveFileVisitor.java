package ru.ifmo.rain.smirnov.walk;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;

public class RecursiveFileVisitor extends SimpleFileVisitor<Path> {
    private final static int BUFFER_SIZE = 4096;
    private final BufferedWriter bufferedWriter;

    RecursiveFileVisitor(BufferedWriter writer) {
        this.bufferedWriter = writer;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        try {
            bufferedWriter.write(String.format("%08x", getFileFNVHash(file)) + " " + file);
            bufferedWriter.newLine();
        } catch (IOException e) {
            System.err.println("Error occurred during writing into output file: " + e.getMessage());
        }
        return CONTINUE;
    }

    private static int getFileFNVHash(Path path) {
        byte[] buff = new byte[BUFFER_SIZE];
        int hash = 0x811c9dc5;
        try (InputStream reader = Files.newInputStream(path)) {
            int count;
            while ((count = reader.read(buff)) != -1) {
                for (int i = 0; i < count; i++) {
                    hash = (hash * 0x01000193) ^ (buff[i] & 0xff);
                }
            }
        } catch (IOException e) {
            System.err.println("Error during calculating hash of file: " + path);
            hash = 0x0000_0000;
        }
        return hash;
    }
}
