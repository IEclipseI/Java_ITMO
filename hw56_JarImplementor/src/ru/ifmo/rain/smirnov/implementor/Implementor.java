package ru.ifmo.rain.smirnov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.security.CodeSource;
import java.util.*;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * Class that implements JarImpler interface
 */
public class Implementor implements JarImpler {
    /**
     * Path separator specified to current OS
     */
    private static final Character PATH_SEPARATOR = File.separatorChar;

    /**
     * Line separator specified to current OS
     */
    private static final String END_LINE = System.lineSeparator();

    /**
     * Default error message during writing file
     */
    private static final String ERROR_DURING_WRITING_FILE = "Error occurred during writing file: ";

    /**
     * Four spaces
     */
    private static final String TAB = "    ";

    /**
     * Default constructor
     */
    public Implementor() {
    }

    /**
     * Main method. First argument should be "-class" or "-jar".
     * Second argument must be classname and in case of "-jar" option should be name of jar file to generate
     *
     * @param args - arguments
     */
    public static void main(String[] args) {
        if (args == null || (args.length != 2 && args.length != 3)) {
            System.out.println("Wrong arguments format");
            return;
        }

        for (String arg : args) {
            if (arg == null) {
                System.out.println("Null argument appeared");
                return;
            }
        }
        Implementor jarImplementor = new Implementor();

        try {
            if (args.length == 2) {
                jarImplementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else if (args.length == 3 && "-jar".equals(args[0])) {
                jarImplementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            } else {
                System.out.println("Wrong arguments format");
            }

        } catch (ClassNotFoundException e) {
            System.out.println("Class not found");
        } catch (InvalidPathException e) {
            System.out.println("Invalid path given");
        } catch (ImplerException e) {
            System.out.println(e.getMessage());
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (token == null || jarFile == null) {
            throw new ImplerException("Not-null arguments expected");
        }
        createDirectories(jarFile);
        Path tmpDir;
        try {
            tmpDir = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "tmp");
        } catch (IOException e) {
            throw new ImplerException("Error during creating temporary directory", e);
        }
        String cp = "";
        try {
            CodeSource source = token.getProtectionDomain().getCodeSource();
            if (source == null) {
                cp = ".";
            } else {
                cp = Path.of(source.getLocation().toURI()).toString();
            }
        } catch (final URISyntaxException e) {
            throw new ImplerException("Cannot resolve classpath" + e.getMessage());
        }
        try {
            implement(token, tmpDir);
            JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
            String[] args = new String[]{
                    "-cp",
                    System.getProperty("java.class.path") + File.pathSeparator
                            + tmpDir + File.pathSeparator +
                            cp,
                    tmpDir.resolve(getFilePath(token, tmpDir, ".java")).toString()
            };

            if (javaCompiler == null || javaCompiler.run(null, null, null, args) != 0) {
                throw new ImplerException("Error during compiling generated java classes");
            }

            Manifest manifest = new Manifest();
            Attributes attributes = manifest.getMainAttributes();
            attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");

            try (JarOutputStream jarWriter = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
                jarWriter.putNextEntry(new ZipEntry(token.getCanonicalName().replace('.', '/') + "Impl.class"));
                Files.copy(tmpDir.resolve(getFilePath(token, tmpDir, ".class")), jarWriter);
            } catch (IOException e) {
                throw new ImplerException("Writing a jar file an error occurred", e);
            }

        } finally {
            try {
                Files.walk(tmpDir)
                        .map(Path::toFile)
                        .sorted(Comparator.reverseOrder())
                        .forEach(File::delete);
            } catch (IOException e) {
                throw new ImplerException("Failed deleting temporary files in " + tmpDir.toString());
            }
        }
    }

    /**
     * Class that wraps {@link Method} and provides {@link MethodWrapper#equals(Object)} and
     * {@link MethodWrapper#hashCode()} depending on wrapped method
     */
    private class MethodWrapper {
        /**
         * Wrapped method
         */
        final private Method method;

        /**
         * Constructor receiving method for wrap
         *
         * @param method method to be wrapped
         */
        private MethodWrapper(Method method) {
            this.method = method;
        }

        /**
         * Getter for wrapped method
         *
         * @return wrapped method
         */
        private Method getMethod() {
            return method;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj instanceof MethodWrapper) {
                MethodWrapper other = (MethodWrapper) obj;
                return Arrays.equals(method.getParameterTypes(), other.method.getParameterTypes())
                        && method.getName().equals(other.method.getName())
                        && method.getReturnType().equals(other.method.getReturnType());
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return 31 * 31 * Arrays.hashCode(method.getParameterTypes())
                    + 31 * method.getName().hashCode()
                    + method.getReturnType().hashCode();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Not-null arguments expected");
        }
        if (token.equals(Enum.class) || token.isPrimitive() || token.isArray() || token.isEnum() || Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("bad token");
        }
        Path filepath = getFilePath(token, root, ".java");
        createDirectories(filepath);
        try (Writer writer = Files.newBufferedWriter(filepath)) {
            writePackage(writer, token);
            writeClassName(writer, token);
            if (!token.isInterface())
                writeConstructors(writer, token);
            writeMethods(writer, token);
            writer.write("}");
        } catch (IOException e) {
            throw new ImplerException(ERROR_DURING_WRITING_FILE + e.getMessage());
        }
    }

    /**
     * Writes constructors of token into file associated with writer
     *
     * @param writer writer associated with implemented class file
     * @param token  implemented class token
     * @throws ImplerException if given token has no non-private constructors
     *                         or any error occurs during writing into file
     */
    private void writeConstructors(Writer writer, Class<?> token) throws ImplerException {
        Constructor<?>[] constructors = token.getDeclaredConstructors();
        List<Constructor<?>> constructors1 = Arrays.stream(constructors)
                .filter(x -> !Modifier.isPrivate(x.getModifiers())).collect(Collectors.toList());
        if (constructors1.size() == 0) {
            throw new ImplerException("No constructors in non abstract class");
        }
        for (Constructor<?> constructor : constructors1) {
            try {
                writer.write(getExecutable(constructor));
            } catch (IOException e) {
                throw new ImplerException(ERROR_DURING_WRITING_FILE + e.getMessage());
            }
        }
    }

    /**
     * Gets methods by applying func on given token, then
     * Wraps them by {@link MethodWrapper} and inserts into the given {@link Set}
     *
     * @param methods set for insertions
     * @param token   class token
     * @param func    Function receiving token and returning array of methods
     */
    private void addAllAbstractMethods(Set<MethodWrapper> methods, Class<?> token,
                                       Function<Class<?>, Method[]> func) {
        Arrays.stream(func.apply(token))
                .filter(x -> Modifier.isAbstract(x.getModifiers()))
                .map(MethodWrapper::new)
                .collect(Collectors.toCollection(() -> methods));
    }

    /**
     * Extracts all {@link Modifier#ABSTRACT} methods of given token
     *
     * @param token class token
     * @return set of abstract methods
     */
    private Set<Method> getAllAbstractMethods(Class<?> token) {
        Set<MethodWrapper> methods = new HashSet<>();
        addAllAbstractMethods(methods, token, Class::getMethods);
        while (token != null) {
            addAllAbstractMethods(methods, token, Class::getDeclaredMethods);
            token = token.getSuperclass();
        }
        return methods.stream().map(MethodWrapper::getMethod).collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Writes all methods of class implemented token's class into file associated with writer
     *
     * @param writer writer associated with implementing class file
     * @param token  implementing class token
     * @throws ImplerException if any errors occurs during writing
     */
    private void writeMethods(Writer writer, Class<?> token) throws ImplerException {
        Set<Method> methods = getAllAbstractMethods(token);
        for (Method m : methods) {
            try {
                writer.write(getExecutable(m));
            } catch (IOException e) {
                throw new ImplerException(ERROR_DURING_WRITING_FILE + e.getMessage());
            }
        }
    }

    /**
     * Multiplies {@link Implementor#TAB}
     *
     * @param count number of repetition
     * @return {@link Implementor#TAB} repeated count times
     */
    private String getTabs(int count) {
        return TAB.repeat(count);
    }

    /**
     * Converts the given array of tokens into string.
     * Every token is separated by ", "
     *
     * @param exceptions given array of tokens
     * @return string representation of given token's array
     */
    private String getExceptions(Class<?>[] exceptions) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Class<?> exception : exceptions) {
            stringBuilder.append(exception.getCanonicalName()).append(", ");
        }
        return stringBuilder.substring(0, stringBuilder.length() - 2);
    }

    /**
     * Converts the given executable into default string representation.
     * It includes return type for functions or methods, name, checked exceptions
     * throwing by it, its arguments and body
     *
     * @param exec given executable
     * @return default string representation of given executable
     */
    private String getExecutable(Executable exec) {
        return getTabs(1) + Modifier.toString(exec.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT)
                + " " + getReturnTypeAndName(exec)
                + "(" + getArguments(exec) + ")"
                + (exec.getExceptionTypes().length == 0
                ? ""
                : " throws " + getExceptions(exec.getExceptionTypes()) + " ") + "{" + END_LINE
                + getBody(exec) + END_LINE
                + getTabs(1) + "}" + END_LINE + END_LINE;
    }

    /**
     * Converts the given executable into string containing its default body.
     * The body will contains only return statement with default value
     * concerted with return type of given executable for functions and methods.
     * For constructors it also will contains the call of super method with correct arguments
     * related with given executable arguments
     *
     * @param exec given executable
     * @return default body of given executable
     */
    private String getBody(Executable exec) {
        return (exec instanceof Constructor ?
                getTabs(2) + "super(" + getArguments(exec, false) + ");" + END_LINE : "") +
                getTabs(2) + "return " + getReturn(exec) + ";";
    }

    /**
     * Converts the given executable into string containing all its arguments accompanied by their
     * types and separated by ", ".
     * Equivalently to call {@link Implementor#getArguments(Executable, boolean)} with true flag
     *
     * @param exec given executable
     * @return arguments of executable separated by ", "
     */
    private String getArguments(Executable exec) {
        return getArguments(exec, true);
    }

    /**
     * Converts the given executable into string containing all its arguments separated by ", ".
     * If flag is true they will be accompanied by their types
     *
     * @param exec     given executable
     * @param withType should type be written before the variables names
     * @return arguments of executable separated by ", "
     */
    private String getArguments(Executable exec, boolean withType) {
        StringBuilder sb = new StringBuilder();
        Parameter[] parameters = exec.getParameters();
        for (Parameter parameter : parameters) {
            sb
                    .append(withType ? parameter.getType().getCanonicalName() : "")
                    .append(" ").append(parameter.getName()).append(", ");
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 2) : sb.toString();
    }

    /**
     * Converts the given executable into string containing return type and name
     *
     * @param exec given executable
     * @return string containing return type and name of the given executable
     */
    private String getReturnTypeAndName(Executable exec) {
        if (exec instanceof Method) {
            Method m = (Method) exec;
            return m.getReturnType().getCanonicalName() + " " + m.getName();
        } else {
            return exec.getDeclaringClass().getSimpleName() + "Impl";
        }
    }

    /**
     * Converts the given executable into string containing return with default value
     *
     * @param exec given executable
     * @return "return" + default value for given executable("" for void functions and constructors) + ";"
     */
    private String getReturn(Executable exec) {
        if (exec instanceof Method) {
            Method m = (Method) exec;
            if (m.getReturnType() == void.class) {
                return "";
            } else if (m.getReturnType().isPrimitive()) {
                return m.getReturnType() == boolean.class ? "false" : "0";
            }
            return "null";
        }
        return "";
    }

    /**
     * Writes package of token into file associated with writer
     *
     * @param writer writer associated with implemented class file
     * @param token  implemented class token
     * @throws ImplerException if any errors occurs during writing
     */
    private void writePackage(Writer writer, Class<?> token) throws ImplerException {
        try {
            writer.write("package " + token.getPackageName() + ";" + END_LINE + END_LINE);
        } catch (IOException e) {
            throw new ImplerException(ERROR_DURING_WRITING_FILE + e.getMessage());
        }
    }

    /**
     * Creates nonexistent directories in given path
     *
     * @param path given path
     * @throws ImplerException if any errors occurs during creating directories
     */
    private void createDirectories(Path path) throws ImplerException {
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new ImplerException("Can't create directories in path: " + path);
            }
        }
    }

    /**
     * Converts the given token into the path of source file
     * potentially implemented and resolved against the given path
     * and add the given file extension
     *
     * @param token  given token
     * @param root   path to be resolve against
     * @param suffix file extension
     * @return the resulting path
     */
    private Path getFilePath(Class<?> token, Path root, String suffix) {
        return root.resolve(token.getPackageName().replace('.', PATH_SEPARATOR))
                .resolve(token.getSimpleName() + "Impl" + suffix);
    }

    /**
     * Writes definition of class implementing token's class into file associated with writer
     *
     * @param writer writer associated with implementing class file
     * @param token  implementing class token
     * @throws ImplerException if any errors occurs during writing
     */
    private void writeClassName(Writer writer, Class<?> token) throws ImplerException {
        try {
            writer.write("public class " + token.getSimpleName() + "Impl "
                    + (token.isInterface() ? "implements " : "extends ")
                    + token.getSimpleName()
                    + " {" + END_LINE);
        } catch (IOException e) {
            throw new ImplerException(ERROR_DURING_WRITING_FILE + e.getMessage());
        }
    }
}
