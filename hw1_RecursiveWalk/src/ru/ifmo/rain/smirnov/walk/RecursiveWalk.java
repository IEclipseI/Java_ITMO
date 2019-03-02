package ru.ifmo.rain.smirnov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RecursiveWalk {
    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Incorrect arguments, needed two: <input filename> <output filename>");
        } else {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), StandardCharsets.UTF_8))) {
                try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1]), StandardCharsets.UTF_8))) {
                    try {
                        String path;
                        while ((path = br.readLine()) != null) {
                            RecursiveFileVisitor visitor = new RecursiveFileVisitor(bw);
                            Path path1;
                            try {
                                path1 = Paths.get(path);
                                try {
                                    Files.walkFileTree(path1, visitor);
                                } catch (IOException e) {
                                    System.err.println("Error occurred during scanning directory \"" + path1 + "\", error: " + e.getMessage());
                                    try {
                                        bw.write(String.format("%08x", 0) + " " + path);
                                        bw.newLine();
                                    } catch (IOException r) {
                                        System.err.println("Error occurred during writing output file: " + r.getMessage());
                                    }
                                }
                            } catch (InvalidPathException e) {
                                System.err.println("Invalid path: " + path);
                                try {
                                    bw.write(String.format("%08x", 0) + " " + path);
                                    bw.newLine();
                                } catch (IOException r) {
                                    System.err.println("Error during writing output file: " + e.getMessage());
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error occurred during reading input file: " + e.getMessage());
                    }
                } catch (FileNotFoundException e) {
                    System.err.println("The file exists but is a directory rather than a regular file, " +
                            "does not exist but cannot be created, " +
                            "or cannot be opened for any other reason: " + args[1]);
                } catch (IOException e) {
                    System.err.println("Error occurred: " + e.getMessage());
                }
            } catch (FileNotFoundException e) {
                System.err.println("File is not found: " + args[0]);
            } catch (IOException e) {
                System.err.println("Error occurred: " + e.getMessage());
            }
        }
    }
}
