package ru.ifmo.rain.smirnov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Implementor implements Impler {
    private static final Character PATH_SEPARATOR = File.separatorChar;
    private static final String END_LINE = System.lineSeparator();
    private static final String ERROR_DURING_WRITING_FILE = "Error occurred during writing file: ";
    private static final String TAB = "    ";

    public static void main(String[] args) {
        if (args == null || args.length != 1 || args[0] == null) {
            System.err.println("Bad arguments, must be one: <[interface|class]name>");
            return;
        }
        try {
            new Implementor().implement(Class.forName(args[0]), Path.of("."));
        } catch (ImplerException e) {
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Can not find given class: " + args[0]);
        }
    }

    private class MethodWrapper {
        final private Method method;

        private MethodWrapper(Method method) {
            this.method = method;
        }

        private Method getMethod() {
            return method;
        }

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

        @Override
        public int hashCode() {
            return 31 * 31 * Arrays.hashCode(method.getParameterTypes())
                    + 31 * method.getName().hashCode()
                    + method.getReturnType().hashCode();
        }

    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token.isPrimitive() || token.isArray() || token.isEnum() || Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("bad token");
        }
        Path filepath = getFilePath(token, root);
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

    private void writeConstructors(Writer writer, Class<?> token) throws ImplerException {
        Constructor<?>[] constructors = token.getConstructors();
        if (constructors.length == 0) {
            throw new ImplerException("No constructors in non abstract class");
        }
        for (Constructor<?> constructor : constructors) {
            try {
                writer.write(getExecutable(constructor));
            } catch (IOException e) {
                throw new ImplerException(ERROR_DURING_WRITING_FILE + e.getMessage());
            }
        }
    }

    private void addAllAbstractMethods(Set<MethodWrapper> methods, Class<?> token,
                                       Function<Class<?>, Method[]> func) {
        Arrays.stream(func.apply(token))
                .filter(x -> Modifier.isAbstract(x.getModifiers()))
                .map(MethodWrapper::new)
                .collect(Collectors.toCollection(() -> methods));
    }

    private Set<Method> getAllAbstractMethods(Class<?> token) {
        Set<MethodWrapper> methods = new HashSet<>();
        addAllAbstractMethods(methods, token, Class::getMethods);
        while (token != null) {
            addAllAbstractMethods(methods, token, Class::getDeclaredMethods);
            token = token.getSuperclass();
        }
        return methods.stream().map(MethodWrapper::getMethod).collect(Collectors.toCollection(HashSet::new));
    }

    private void writeMethods(Writer wr, Class<?> token) throws ImplerException {
        Set<Method> methods = getAllAbstractMethods(token);
        for (Method m : methods) {
            try {
                wr.write(getExecutable(m));
            } catch (IOException e) {
                throw new ImplerException(ERROR_DURING_WRITING_FILE + e.getMessage());
            }
        }
    }

    private String getTabs(int count) {
        return TAB.repeat(count);
    }

    private String getExceptions(Class<?>[] exceptions) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Class<?> exception : exceptions) {
            stringBuilder.append(exception.getCanonicalName()).append(", ");
        }
        return stringBuilder.substring(0, stringBuilder.length() - 2);
    }

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

    private String getBody(Executable exec) {
        return (exec instanceof Constructor ?
                getTabs(2) + "super(" + getArguments(exec, false) + ");" + END_LINE : "") +
                getTabs(2) + "return " + getReturn(exec) + ";";
    }

    private String getArguments(Executable exec) {
        return getArguments(exec, true);
    }

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

    private String getReturnTypeAndName(Executable exec) {
        if (exec instanceof Method) {
            Method m = (Method) exec;
            return m.getReturnType().getCanonicalName() + " " + m.getName();
        } else {
            return exec.getDeclaringClass().getSimpleName() + "Impl";
        }
    }

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

    private void writePackage(Writer wr, Class<?> token) throws ImplerException {
        try {
            wr.write("package " + token.getPackageName() + ";" + END_LINE + END_LINE);
        } catch (IOException e) {
            throw new ImplerException(ERROR_DURING_WRITING_FILE + e.getMessage());
        }
    }

    private void createDirectories(Path path) throws ImplerException {
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new ImplerException("Can't create directories in path: " + path);
            }
        }
    }

    private Path getFilePath(Class<?> token, Path root) {
        return root.resolve(token.getPackageName().replace('.', PATH_SEPARATOR))
                .resolve(token.getSimpleName() + "Impl.java");
    }

    private void writeClassName(Writer wr, Class<?> token) throws ImplerException {
        try {
            wr.write("public class " + token.getSimpleName() + "Impl "
                    + (token.isInterface() ? "implements " : "extends ")
                    + token.getSimpleName()
                    + " {" + END_LINE);
        } catch (IOException e) {
            throw new ImplerException(ERROR_DURING_WRITING_FILE + e.getMessage());
        }
    }
}
