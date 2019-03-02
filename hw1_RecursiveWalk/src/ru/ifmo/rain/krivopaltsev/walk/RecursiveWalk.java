package ru.ifmo.rain.krivopaltsev.walk;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk extends Walk {

    public RecursiveWalk(String input, String output) {
        super(input, output);
    }

    @Override
    protected void calculateHash(Path path, Writer writer) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    calculateHashFile(file, writer);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    writeLine(writer, 0, file.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.out.println("Directory: " + path + " is not existed");
        } catch (SecurityException e) {
            System.out.println("Access to directory: " + path + " is denied");
        }

    }

    public static void main(String[] args) {
        if (!validateArgs(args) || createDirectories(args[1])) {
            return;
        }
        RecursiveWalk walk = new RecursiveWalk(args[0], args[1]);
        walk.start();
    }
}
