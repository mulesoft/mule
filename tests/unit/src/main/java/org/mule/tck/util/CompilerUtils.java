/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.util;

import static java.io.File.pathSeparator;
import static java.util.stream.Collectors.toList;
import static javax.tools.ToolProvider.getSystemJavaCompiler;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.filefilter.TrueFileFilter.TRUE;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.tck.ZipUtils.compress;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.tck.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import org.apache.commons.io.filefilter.NameFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tools to compile Java files into classes, jars and Mule extensions.
 */
public class CompilerUtils {

  private static final Logger logger = LoggerFactory.getLogger(CompilerUtils.class);

  // Class used to compile extension annotations. This class must be in the classpath only when
  // a jar is compiled as an extension.
  private static final String EXTENSION_ANNOTATION_PROCESSOR_CLASSNAME =
      "org.mule.runtime.module.extension.internal.resources.ExtensionResourcesGeneratorAnnotationProcessor";

  /**
   * Base class to create compiler utilities.
   * @param <T> class of the implemented compiler
   */
  private static abstract class AbstractCompiler<T extends AbstractCompiler> {

    protected File[] requiredJars = {};
    protected File[] sources = {};

    /**
     * @return current instance. Used just to avoid compilation warnings.
     */
    protected abstract T getThis();

    /**
     * Adds jar files to the classpath used during the compilation.
     *
     * @param requiredJars jars to include in the classpath. Non null.
     * @return the same compiler instance
     */
    public T dependingOn(File... requiredJars) {
      this.requiredJars = requiredJars;

      return getThis();
    }

    /**
     * @return a folder to write the files resulting from the compilation
     */
    protected File createTargetFolder() {
      try {
        File tempFolder = File.createTempFile(CompilerUtils.class.getSimpleName(), "");
        tempFolder.delete();
        tempFolder.mkdir();
        return tempFolder;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Compiles all the Java sources defined on the compiler.
     *
     * @param targetFolder folder where the compilation result will be written. Non null.
     */
    protected void compileJavaSources(File targetFolder) {
      checkArgument(targetFolder != null, "targetFolder cannot be null");

      CompilerTask compilerTask = new CompilerTaskBuilder().compiling(sources)
          .dependingOn(requiredJars).toTarget(targetFolder).build();
      compilerTask.compile();
    }
  }
  /**
   * Compiles a single Java file into a Java class.
   */
  public static class SingleClassCompiler extends AbstractCompiler<SingleClassCompiler> {

    private File targetFolder;

    /**
     * Compiles a single Java file.
     *
     * @param source file to compile. Non null.
     * @return the compiled class file
     */
    public File compile(File source) {
      checkArgument(source != null, "source cannot be null");

      targetFolder = createTargetFolder();
      sources = new File[] {source};

      compileJavaSources(targetFolder);

      return getCompiledClass(targetFolder, source.getName());
    }

    /**
     * @return the folder where compiled classes where written or null if {@link #compile(File)} was not execute yet.
     */
    public File getTargetFolder() {
      return targetFolder;
    }

    private File getCompiledClass(File targetFolder, String name) {
      String className = name.replace("java", "class");
      Collection<File> classes = listFiles(targetFolder, new NameFileFilter(className), TRUE);
      if (classes.size() > 1) {
        throw new IllegalStateException("Cannot return compiled class as there are more than one compiled class file");
      }
      return classes.iterator().next();
    }

    @Override
    protected SingleClassCompiler getThis() {
      return this;
    }
  }

  /**
   * Base class to create a compiler that compiles multiple source files.
   * @param <T> class of the implemented compiler
   */
  protected static abstract class MultipleFileCompiler<T extends MultipleFileCompiler> extends AbstractCompiler<T> {

    private List<ZipUtils.ZipResource> configuredResources = new ArrayList<>();

    /**
     * Indicates which source file must be compiled.
     * 
     * @param sources source files. Non empty.
     * @return the same compiler instance
     */
    public T compiling(File... sources) {
      checkArgument(sources != null && sources.length > 0, "source cannot be empty");
      this.sources = sources;

      return getThis();
    }

    /**
     * Includes a resource file into the generated JAR file.
     *
     * @param resource resource file. Non empty.
     * @return the same compiler instance
     */
    public T including(File resource, String alias) {

      configuredResources.add(new ZipUtils.ZipResource(resource.getAbsolutePath(), alias));

      return getThis();
    }

    /**
     * Generates a JAR file form the compiled files
     *
     * @param targetFolder folder containing the compiled files. Non null.
     * @param jarName name of the JAR file. Non empty.
     * @return
     */
    protected File compressGeneratedFiles(File targetFolder, String jarName) {
      checkArgument(targetFolder != null, "targetFolder cannot be byll");
      checkArgument(!StringUtils.isEmpty(jarName), "jar name cannot be empty");

      Collection<File> files = listFiles(targetFolder, TRUE, TRUE);
      ZipUtils.ZipResource[] resources = getZipResources(targetFolder, files);
      File targetFile = new File(targetFolder, jarName);
      compress(targetFile, resources);
      return targetFile;
    }

    private ZipUtils.ZipResource[] getZipResources(File targetFolder, Collection<File> classes) {
      List<ZipUtils.ZipResource> compiledResources = classes.stream()
          .map(f -> new ZipUtils.ZipResource(f.getAbsolutePath(), getRelativePath(targetFolder, f))).collect(toList());
      compiledResources.addAll(configuredResources);

      return compiledResources.toArray(new ZipUtils.ZipResource[0]);
    }

    private String getRelativePath(File targetFolder, File file) {
      final StringJoiner pathJoiner = new StringJoiner("/");
      for (Path targetFolderPathElement : targetFolder.toPath().relativize(file.toPath())) {
        pathJoiner.add(targetFolderPathElement.toString());
      }
      return pathJoiner.toString();
    }

  }

  /**
   * Compiles a set of Java sources into a Jar file.
   */
  public static class JarCompiler extends MultipleFileCompiler<JarCompiler> {


    /**
     * Compiles all the provided sources generating a JAR file.
     *
     * @param jarName name of the JAR file to create. Non empty.
     * @return the created file.
     */
    public File compile(String jarName) {
      File targetFolder = createTargetFolder();

      compileJavaSources(targetFolder);

      return compressGeneratedFiles(targetFolder, jarName);
    }


    @Override
    protected JarCompiler getThis() {
      return this;
    }
  }

  /**
   * Compiles a set of Java sources defining a Mule extension into a Jar file.
   */
  public static class ExtensionCompiler extends MultipleFileCompiler<ExtensionCompiler> {

    @Override
    protected ExtensionCompiler getThis() {
      return this;
    }

    /**
     * Compiles all the provided sources generating a JAR file.
     *
     * @param jarName name of the JAR file to create. Non empty.
     * @param  extensionVersion version of the extension being compiled. Non empty.
     * @return the created file.
     */
    public File compile(String jarName, String extensionVersion) {
      checkArgument(!StringUtils.isEmpty(jarName), "jarName cannot be empty");
      checkArgument(!StringUtils.isEmpty(extensionVersion), "extensionVersion cannot be empty");

      File targetFolder = createTargetFolder();

      compileJavaSources(targetFolder);

      processExtensionAnnotations(targetFolder, extensionVersion);

      return compressGeneratedFiles(targetFolder, jarName);
    }

    private void processExtensionAnnotations(File targetFolder, String extensionVersion) {
      URLClassLoader urlClassLoader = createExtensionClassLoader(targetFolder);

      ClassUtils.withContextClassLoader(urlClassLoader, () -> {
        File metaInfFolder = new File(targetFolder, "META-INF");
        metaInfFolder.mkdir();
        CompilerTask compilerTask =
            new CompilerTaskBuilder().compiling(sources).dependingOn(requiredJars)
                .withProperty("extension.version", extensionVersion)
                .processingAnnotations(EXTENSION_ANNOTATION_PROCESSOR_CLASSNAME)
                .toTarget(metaInfFolder).build();

        compilerTask.compile();
      });
    }

    private URLClassLoader createExtensionClassLoader(File targetFolder) {
      URLClassLoader urlClassLoader;
      try {
        urlClassLoader = new URLClassLoader(new URL[] {targetFolder.toURL()});
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
      return urlClassLoader;
    }
  }

  private static class CompilerTaskBuilder {

    private File target;
    private File[] sources = {};
    private File[] jarFiles = {};
    private String annotationProcessorClassName;
    private final List<String> processProperties = new ArrayList<>();

    public CompilerTaskBuilder toTarget(File target) {
      this.target = target;

      return this;
    }

    public CompilerTaskBuilder dependingOn(File... jarFiles) {
      this.jarFiles = jarFiles;

      return this;
    }

    public CompilerTaskBuilder compiling(File... sources) {
      this.sources = sources;

      return this;
    }

    public CompilerTaskBuilder withProperty(String name, String value) {
      processProperties.add("-A" + name + "=" + value);
      return this;
    }

    public CompilerTaskBuilder processingAnnotations(String annotationProcessorClassName) {
      this.annotationProcessorClassName = annotationProcessorClassName;

      return this;
    }

    public CompilerTask build() {
      if (sources.length == 0) {
        throw new IllegalArgumentException("Must define at least a source file to compile");
      }

      return new CompilerTask(sources, getOptions());
    }

    private List<String> getOptions() {

      List<String> options = new ArrayList<>();

      if (logger.isInfoEnabled()) {
        options.add("-verbose");
      }

      if (annotationProcessorClassName == null) {
        // Disables annotation processing to avoid warnings
        options.add("-proc:none");
      } else {
        options.add("-processor");
        options.add(annotationProcessorClassName);
        options.add("-proc:only");
      }

      if (target != null) {
        options.add("-d");
        options.add(target.getAbsolutePath());
      }

      if (jarFiles.length > 0) {
        // Adds same classpath as the one used on the runner
        String classPath = System.getProperty("java.class.path");
        // Adds extra jars files required to compile the source classes
        for (File jarFile : jarFiles) {
          classPath = classPath + pathSeparator + jarFile.getAbsolutePath();
        }
        options.addAll(Arrays.asList("-classpath", classPath));
      }

      options.addAll(processProperties);

      return options;
    }
  }

  private static class CompilerTask {

    private final File[] sources;
    private final List<String> options;

    private CompilerTask(File[] sources, List<String> options) {
      this.sources = sources;
      this.options = options;
    }

    public void compile() {
      final JavaCompiler compiler = getSystemJavaCompiler();
      final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
      try {

        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(sources);

        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, compilationUnits);

        Boolean status = task.call();
        if (!status) {
          throw new RuntimeException("Compiler task finished with error. Enable logging to find more information");
        }
      } catch (Throwable e) {
        logger.error("Error processing compilation task", e);
        throw e;
      } finally {
        try {
          fileManager.close();
        } catch (IOException e) {
          // Ignore
        }
      }
    }
  }
}
