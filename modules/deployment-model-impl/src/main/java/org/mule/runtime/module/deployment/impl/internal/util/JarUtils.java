/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.util;

import static org.mule.runtime.core.internal.util.StandaloneServerUtils.getMuleHome;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.Paths.get;

import org.mule.runtime.core.api.util.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JarUtils {

  private static final String MULE_MODULE_FILENAME = "lib" + File.separator + "module";
  private static final String MULE_LIB_FILENAME = "lib" + File.separator + "mule";
  private static final String MULE_HOME = getMuleHome().map(File::getAbsolutePath).orElse(null);

  private static final int MAX_ENTRY_SIZE = 1024 * 1024 * 1024; // 1GB limit
  private static final int MAX_ENTRIES = 10000; // Maximum number of entries in a JAR

  public static final String MULE_LOCAL_JAR_FILENAME = "mule-local-install.jar";

  private static final Logger logger = LoggerFactory.getLogger(JarUtils.class);

  private JarUtils() {
    // utility class only
  }

  /**
   * Creates an URL to a path within a jar file.
   *
   * @param jarFile  the jar file
   * @param filePath the path within the jar file
   * @return an URL to the {@code filePath} within the {@code jarFile}
   * @throws MalformedURLException    if the provided {@code filePath} is malformed
   * @throws IllegalArgumentException if the filePath contains path traversal attempts
   */
  public static URL getUrlWithinJar(File jarFile, String filePath) throws MalformedURLException {
    validatePath(filePath);
    return new URL("jar:" + jarFile.toURI().toString() + "!/" + filePath);
  }

  /**
   * Gets all the URL of files within a directory
   *
   * @param file      the jar file
   * @param directory the directory within the jar file
   * @return a collection of URLs to files within the directory {@code directory}. Empty collection if the directory does not
   *         exists or is empty.
   * @throws IOException              if there was a problem reading from the jar file
   * @throws IllegalArgumentException if the directory path contains path traversal attempts
   */
  public static List<URL> getUrlsWithinJar(File file, String directory) throws IOException {
    validatePath(directory);
    List<URL> urls = new ArrayList<>();
    try (JarFile jarFile = new JarFile(file)) {
      Enumeration<JarEntry> entries = jarFile.entries();
      while (entries.hasMoreElements()) {
        JarEntry jarEntry = entries.nextElement();
        if (!jarEntry.isDirectory() && jarEntry.getName().startsWith(directory + "/")) {
          urls.add(getUrlWithinJar(file, jarEntry.getName()));
        }
      }
    }
    return urls;
  }

  public static Map<String, Object> readJarFileEntries(File jarFile) throws IOException {
    Map<String, Object> entries = new LinkedHashMap<>();
    logger.debug("Reading jar entries from {}", jarFile.getAbsolutePath());
    try (JarFile jarFileWrapper = new JarFile(jarFile)) {
      Enumeration<JarEntry> iter = jarFileWrapper.entries();
      int entryCount = 0;

      while (iter.hasMoreElements()) {
        if (++entryCount > MAX_ENTRIES) {
          throw new IOException("JAR file contains too many entries: " + entryCount);
        }

        ZipEntry zipEntry = iter.nextElement();
        validatePath(zipEntry.getName());

        if (zipEntry.getSize() > MAX_ENTRY_SIZE) {
          throw new IOException("JAR entry too large: " + zipEntry.getName() + " (" + zipEntry.getSize() + " bytes)");
        }

        try (InputStream entryStream = jarFileWrapper.getInputStream(zipEntry);
            ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream()) {
          IOUtils.copy(entryStream, byteArrayStream);
          entries.put(zipEntry.getName(), byteArrayStream.toByteArray());
          logger.debug("Read jar entry {} from {}", zipEntry.getName(), jarFile.getAbsolutePath());
        }
      }
    }
    return entries;
  }

  public static void appendJarFileEntries(File jarFile, Map<String, Object> entries) throws IOException {
    // Validate all entry paths before proceeding
    for (String entryPath : entries.keySet()) {
      validatePath(entryPath);
    }

    Map<String, Object> combinedEntries = readJarFileEntries(jarFile);
    combinedEntries.putAll(entries);
    File tmpJarFile = createTempFile(jarFile.getName(), null).toFile();
    createJarFileEntries(tmpJarFile, combinedEntries);
    jarFile.delete();
    FileUtils.renameFile(tmpJarFile, jarFile);
  }

  public static void createJarFileEntries(File jarFile, Map<String, Object> entries) throws IOException {
    logger.debug("Creating jar file {}", jarFile.getAbsolutePath());

    if (entries == null || entries.isEmpty()) {
      return;
    }

    try (FileOutputStream fileStream = new FileOutputStream(jarFile);
        JarOutputStream jarStream = new JarOutputStream(fileStream)) {

      int entryCount = 0;
      Iterator<String> iter = entries.keySet().iterator();
      while (iter.hasNext()) {
        if (++entryCount > MAX_ENTRIES) {
          throw new IOException("Too many entries to write to JAR: " + entryCount);
        }

        String jarFilePath = iter.next();
        validatePath(jarFilePath);
        createForJarEntry(jarFile, entries, jarStream, jarFilePath);
      }

      jarStream.flush();
      fileStream.getFD().sync();
    }
  }

  private static void createForJarEntry(File jarFile, Map<String, Object> entries, JarOutputStream jarStream,
                                        String jarFilePath)
      throws IOException {
    Object content = entries.get(jarFilePath);

    JarEntry entry = new JarEntry(jarFilePath);
    jarStream.putNextEntry(entry);

    logger.debug("Adding jar entry {} to {}", jarFilePath, jarFile.getAbsolutePath());

    if (content instanceof String stringContent) {
      writeJarEntry(jarStream, stringContent.getBytes());
    } else if (content instanceof byte[] byteArrayContent) {
      writeJarEntry(jarStream, byteArrayContent);
    } else if (content instanceof File fileContent) {
      writeJarEntry(jarStream, fileContent);
    }
  }

  private static void writeJarEntry(OutputStream stream, byte[] entry) throws IOException {
    if (entry.length > MAX_ENTRY_SIZE) {
      throw new IOException("Entry too large: " + entry.length + " bytes");
    }
    stream.write(entry, 0, entry.length);
  }

  private static void writeJarEntry(OutputStream stream, File entry) throws IOException {
    if (entry.length() > MAX_ENTRY_SIZE) {
      throw new IOException("File too large: " + entry.length() + " bytes");
    }
    try (FileInputStream fileContentStream = new FileInputStream(entry)) {
      IOUtils.copy(fileContentStream, stream);
    }
  }

  /**
   * Validates a path to prevent path traversal attacks.
   *
   * @param path the path to validate
   * @throws IllegalArgumentException if the path contains path traversal attempts
   */
  private static void validatePath(String path) {
    if (path == null) {
      throw new IllegalArgumentException("Path cannot be null");
    }

    Path normalizedPath = get(path).normalize();
    if (normalizedPath.startsWith("..") || normalizedPath.toString().contains("..")) {
      throw new IllegalArgumentException("Path traversal not allowed: " + path);
    }
  }

  public static File getMuleHomeFile() {
    return new File(MULE_HOME);
  }

  public static File getMuleLibDir() {
    return new File(MULE_HOME + File.separator + MULE_LIB_FILENAME);
  }

  public static File getMuleModuleDir() {
    return new File(MULE_HOME + File.separator + MULE_MODULE_FILENAME);
  }

  public static File getMuleLocalJarFile() {
    return new File(getMuleLibDir(), MULE_LOCAL_JAR_FILENAME);
  }
}
