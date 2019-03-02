package ru.ifmo.rain.krivopaltsev.walk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Walk {
    private Path input;
    private Path output;

    public Walk(String inputStr, String outputStr) {
        try {
            this.input = Paths.get(inputStr);
        } catch (InvalidPathException e) {
            System.out.println("Incorrect input file: " + inputStr);
        }
        try {
            this.output = Paths.get(outputStr);
        } catch (InvalidPathException e) {
            System.out.println("Incorrect output file: " + outputStr);
        }
    }

    public void start() {
        if (input == null || output == null)
            return;
        try (BufferedReader reader = Files.newBufferedReader(input)) {
            try (BufferedWriter writer = Files.newBufferedWriter(output)) {
                String filename;
                while ((filename = reader.readLine()) != null) {
                    try {
                        calculateHash(Paths.get(filename), writer);
                    } catch (InvalidPathException e) {
                        writeLine(writer, 0, filename);
                    }
                }
            } catch (IOException e) {
                System.out.println("Output file is not existed");
            } catch (SecurityException e) {
                System.out.println("Access to output file is denied");
            }
        } catch (IOException e) {
            System.out.println("Input file is not existed");
        } catch (SecurityException e) {
            System.out.println("Access to input file is denied");
        }
    }


    protected void writeLine(Writer writer, int hash, String name) {
        try {
            writer.write(String.format("%08x %s%s", hash, name, System.lineSeparator()));
        } catch (IOException e) {
            System.out.println("Error: writing hash of file: " + name);
        }
    }

    protected void calculateHashFile(Path path, Writer writer) {
        FNVHash hash = new FNVHash();
        try (InputStream hashReader = Files.newInputStream(path)) {
            int ch;
            byte[] buf = new byte[1 << 18];
            while ((ch = hashReader.read(buf)) >= 0) {
                hash.hash32(buf, ch);
            }
            writeLine(writer, hash.getHash(), path.toString());
        } catch (IOException | SecurityException e) {
            writeLine(writer,0, path.toString());
        }
    }

    protected void calculateHash(Path path, Writer writer) {
        calculateHashFile(path, writer);
    }

    protected static boolean createDirectories(String path){
        Path dirPath = null;
        try {
            Path p = Paths.get(path);
            dirPath = p.getParent();
        } catch (InvalidPathException e) {
            System.out.println("Wrong path to output file");
        }

        if (dirPath != null) {
            try {
                Files.createDirectories(dirPath);
            } catch (IOException e) {
                System.out.println("Access to output file is denied");
                return false;
            }
        }
        return true;
    }


    protected static boolean validateArgs(String[] args) {
        if (args == null) {
            System.out.println("Arguments cannot be null");
            return false;
        }
        if (args.length != 2) {
            System.out.println("We need 2 arguments");
            return false;
        }
        if (args[0] == null) {
            System.out.println("Input file cannot be null");
            return false;
        }
        if (args[1] == null) {
            System.out.println("Output file cannot be null");
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        if (!validateArgs(args) || createDirectories(args[1])) {
            return;
        }
        Walk walk = new Walk(args[0], args[1]);
        walk.start();
    }
}