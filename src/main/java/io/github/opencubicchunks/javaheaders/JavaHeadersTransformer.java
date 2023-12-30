package io.github.opencubicchunks.javaheaders;

import static org.objectweb.asm.Opcodes.*;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import io.github.opencubicchunks.dasm.MappingsProvider;
import io.github.opencubicchunks.dasm.RedirectsParser.ClassTarget;
import io.github.opencubicchunks.dasm.RedirectsParser.RedirectSet;
import io.github.opencubicchunks.dasm.Transformer;
import io.github.opencubicchunks.dasm.TypeRedirect;
import io.github.opencubicchunks.javaheaders.exceptions.HeaderNotImplementedException;
import org.gradle.api.GradleException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

class CustomClassWriter {
    private final Transformer transformer = new Transformer(MappingsProvider.IDENTITY, true);

    private final RedirectSet redirectSet;

    public CustomClassWriter(RedirectSet redirectSet) {
        this.redirectSet = redirectSet;
    }

    /**
     * @param inputClassNode input class to transform
     * @return If not present, the class should not be added to the output
     */
    public Optional<ClassNode> transformClass(ClassNode inputClassNode) {

        if (redirectSet.getTypeRedirects().stream().anyMatch(typeRedirect -> typeRedirect.srcClassName().equals(inputClassNode.name.replace("/", ".")))) {
            return Optional.empty();
        }

        ClassTarget classTarget = new ClassTarget(inputClassNode.name);
        classTarget.targetWholeClass();
        transformer.transformClass(inputClassNode, classTarget, Collections.singletonList(redirectSet));

        return Optional.of(inputClassNode);
    }
}

public class JavaHeadersTransformer {
    public static void transformCoreLibrary(Config config, File coreJar, File outputCoreJar) {
        List<ClassNode> classNodes;
        try {
            classNodes = loadClasses(coreJar);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new GradleException(String.format("Failed to load classes for %s", coreJar), e);
        }
        List<ClassNode> outputClassNodes = new ArrayList<>();

        Set<String> headersImplemented = new HashSet<>(); // headers in the config file the dependent project has declared it implements
        RedirectSet redirectSet = new RedirectSet("");
        config.implementations.forEach((dstClass, headers) -> {
            for (String header : headers) {
                headersImplemented.add(header);
                redirectSet.addRedirect(new TypeRedirect(header.replace(".", "/"), dstClass.replace(".", "/")));
            }
        });

        for (Iterator<ClassNode> iterator = classNodes.iterator(); iterator.hasNext(); ) {
            ClassNode classNode = iterator.next();

            if (classNode.visibleAnnotations == null) {
                continue;
            }

            for (AnnotationNode visibleAnnotation : classNode.visibleAnnotations) {
                if (visibleAnnotation.desc.contains("Lio/github/opencubicchunks/javaheaders/api/Header;")) {
                    iterator.remove(); // remove and header files from the jar
                    String headerClass = classNode.name.replace("/", ".");
                    if (!headersImplemented.contains(headerClass)) {
                        System.err.printf("Header %s is missing an implementation%n\n", headerClass);
                        throw new HeaderNotImplementedException(String.format("Header %s is missing an implementation", headerClass));
                    }
                }
            }
        }

        // transformation
        CustomClassWriter customClassWriter = new CustomClassWriter(redirectSet);
        classNodes.parallelStream().forEach(classNode -> customClassWriter.transformClass(classNode).ifPresent(outputClassNodes::add));

        try {
            saveAsJar(outputClassNodes, coreJar, outputCoreJar);
            JavaHeaders.LOGGER.info(String.format("Writing jar %s\n", outputCoreJar));
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new GradleException(String.format("Failed to write jar for %s", outputCoreJar), e);
        }
    }

    /**
     * Loads all class entries from a jar outputJar
     */
    private static List<ClassNode> loadClasses(File jarFile) throws IOException {
        try (JarFile jar = new JarFile(jarFile); Stream<JarEntry> str = jar.stream()) {
            return str.map(z -> readJarClasses(jar, z)).filter(Optional::isPresent).map(Optional::get)
                    .map(JavaHeadersTransformer::classNodeFromBytes)
                    .collect(Collectors.toList());
        }
    }

    private static ClassNode classNodeFromBytes(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode(ASM9);
        reader.accept(classNode, 0);
        return classNode;
    }

    private static Optional<byte[]> readJarClasses(JarFile jar, JarEntry entry) {
        String name = entry.getName();
        try (InputStream inputStream = jar.getInputStream(entry)){
            if (name.endsWith(".class")) {
                ByteArrayOutputStream output = new ByteArrayOutputStream(inputStream.available());
                byte[] buf = new byte[4096];
                int read;
                while ((read = inputStream.read(buf)) > 0) {
                    output.write(buf, 0, read);
                }
                byte[] bytes = output.toByteArray();
                return Optional.of(bytes);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return Optional.empty();
    }

    /**
     * Takes a list of class nodes and writes them to the output outputJar
     *
     * All non-class entries from the specified input jar are also written to the output jar
     */
    static void saveAsJar(List<ClassNode> classNodes, File inputJar, File outputJar) throws IOException {
        try (JarOutputStream outputStream = new JarOutputStream(Files.newOutputStream(outputJar.toPath()))) {
            Map<String, byte[]> nonClassEntries = loadNonClasses(inputJar);

            // write all non-class entries from the input jar
            for (Map.Entry<String, byte[]> e : nonClassEntries.entrySet()) {
                outputStream.putNextEntry(new ZipEntry(e.getKey()));
                outputStream.write(e.getValue());
                outputStream.closeEntry();
            }

            // write all class nodes
            for (ClassNode classNode : classNodes) {
                ClassWriter writer = new ClassWriter(0);
                classNode.accept(writer);
                byte[] bytes = writer.toByteArray();

                outputStream.putNextEntry(new ZipEntry(classNode.name + ".class"));
                outputStream.write(bytes);
                outputStream.closeEntry();
            }
        }
    }

    /**
     * Loads all NON-class entries from a jar outputJar
     */
    private static void readNonJars(JarFile jar, JarEntry entry, Map<String, byte[]> nonClasses) {
        String name = entry.getName();
        try (InputStream inputStream = jar.getInputStream(entry)){
            if (!name.endsWith(".class")) {
                ByteArrayOutputStream output = new ByteArrayOutputStream(inputStream.available());
                byte[] buf = new byte[4096];
                int read;
                while ((read = inputStream.read(buf)) > 0) {
                    output.write(buf, 0, read);
                }
                byte[] bytes = output.toByteArray();
                nonClasses.put(name, bytes);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<String, byte[]> loadNonClasses(File jarFile) throws IOException {
        Map<String, byte[]> classes = new LinkedHashMap<>();
        JarFile jar = new JarFile(jarFile);
        Stream<JarEntry> str = jar.stream();
        str.forEach(z -> readNonJars(jar, z, classes));
        jar.close();
        return classes;
    }
}