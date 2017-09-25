/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.mule.runtime.core.internal.util.StandaloneServerUtils.getMuleHome;

import org.mule.runtime.core.api.util.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
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

  public static final String MULE_LOCAL_JAR_FILENAME = "mule-local-install.jar";

  private static final Logger logger = LoggerFactory.getLogger(JarUtils.class);

  private JarUtils() {
    // utility class only
  }

  /**
   * Loads the content of a file within a jar into a byte array.
   * 
   * @param jarFile the jar file
   * @param filePath the path to the file within the jar file
   * @return the content of the file as byte array or empty if the file does not exists within the jar file.
   * @throws IOException if there was a problem reading from the jar file.
   */
  public static Optional<byte[]> loadFileContentFrom(File jarFile, String filePath) throws IOException {
    URL jsonDescriptorUrl = getUrlWithinJar(jarFile, filePath);
    /*
     * A specific implementation of JarURLConnection is required to read jar content because not all implementations
     * support ways to disable connection caching. Disabling connection caching is necessary to avoid file descriptor leaks.
     */
    JarURLConnection jarConnection =
        new sun.net.www.protocol.jar.JarURLConnection(jsonDescriptorUrl, new sun.net.www.protocol.jar.Handler());
    jarConnection.setUseCaches(false);
    try (InputStream inputStream = jarConnection.getInputStream()) {
      byte[] byteArray = toByteArray(inputStream);
      return of(byteArray);
    } catch (FileNotFoundException e) {
      return empty();
    }
  }

  /**
   * Loads the content of a file within a jar into a byte array.
   *
   * @param jarFile the jar file
   * @return the content of the file as byte array or empty if the file does not exists within the jar file.
   * @throws IOException if there was a problem reading from the jar file.
   */
  public static Optional<byte[]> loadFileContentFrom(URL jarFile) throws IOException {
    /*
     * A specific implementation of JarURLConnection is required to read jar content because not all implementations
     * support ways to disable connection caching. Disabling connection caching is necessary to avoid file descriptor leaks.
     */
    JarURLConnection jarConnection =
        new sun.net.www.protocol.jar.JarURLConnection(jarFile, new sun.net.www.protocol.jar.Handler());
    jarConnection.setUseCaches(false);
    try (InputStream inputStream = jarConnection.getInputStream()) {
      byte[] byteArray = toByteArray(inputStream);
      return of(byteArray);
    } catch (FileNotFoundException e) {
      return empty();
    }
  }

  /**
   * Creates an URL to a path within a jar file.
   *
   * @param jarFile the jar file
   * @param filePath the path within the jar file
   * @return an URL to the {@code filePath} within the {@code jarFile}
   * @throws MalformedURLException if the provided {@code filePath} is malformed
   */
  public static URL getUrlWithinJar(File jarFile, String filePath) throws MalformedURLException {
    return new URL("jar:" + jarFile.toURI().toString() + "!/" + filePath);
  }

  /**
   * Gets all the URL of files within a directory
   * 
   * @param file the jar file
   * @param directory the directory within the jar file
   * @return a collection of URLs to files within the directory {@code directory}. Empty collection if the directory does not
   *         exists or is empty.
   * @throws IOException
   */
  public static List<URL> getUrlsWithinJar(File file, String directory) throws IOException {
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

  public static LinkedHashMap readJarFileEntries(File jarFile) throws Exception {
    LinkedHashMap entries = new LinkedHashMap();
    JarFile jarFileWrapper = null;
    if (jarFile != null) {
      logger.debug("Reading jar entries from " + jarFile.getAbsolutePath());
      try {
        jarFileWrapper = new JarFile(jarFile);
        Enumeration iter = jarFileWrapper.entries();
        while (iter.hasMoreElements()) {
          ZipEntry zipEntry = (ZipEntry) iter.nextElement();
          InputStream entryStream = jarFileWrapper.getInputStream(zipEntry);
          ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
          try {
            IOUtils.copy(entryStream, byteArrayStream);
            entries.put(zipEntry.getName(), byteArrayStream.toByteArray());
            logger.debug("Read jar entry " + zipEntry.getName() + " from " + jarFile.getAbsolutePath());
          } finally {
            byteArrayStream.close();
          }
        }
      } finally {
        if (jarFileWrapper != null) {
          try {
            jarFileWrapper.close();
          } catch (Exception ignore) {
            logger.debug("Error closing jar file", ignore);
          }
        }
      }
    }
    return entries;
  }

  public static void appendJarFileEntries(File jarFile, LinkedHashMap entries) throws Exception {
    if (entries != null) {
      LinkedHashMap combinedEntries = readJarFileEntries(jarFile);
      combinedEntries.putAll(entries);
      File tmpJarFile = File.createTempFile(jarFile.getName(), null);
      createJarFileEntries(tmpJarFile, combinedEntries);
      jarFile.delete();
      FileUtils.renameFile(tmpJarFile, jarFile);
    }
  }

  public static void createJarFileEntries(File jarFile, LinkedHashMap entries) throws Exception {
    JarOutputStream jarStream = null;
    FileOutputStream fileStream = null;

    if (jarFile != null) {
      logger.debug("Creating jar file " + jarFile.getAbsolutePath());

      try {
        fileStream = new FileOutputStream(jarFile);
        jarStream = new JarOutputStream(fileStream);

        if (entries != null && !entries.isEmpty()) {
          Iterator iter = entries.keySet().iterator();
          while (iter.hasNext()) {
            String jarFilePath = (String) iter.next();
            Object content = entries.get(jarFilePath);

            JarEntry entry = new JarEntry(jarFilePath);
            jarStream.putNextEntry(entry);

            logger.debug("Adding jar entry " + jarFilePath + " to " + jarFile.getAbsolutePath());

            if (content instanceof String) {
              writeJarEntry(jarStream, ((String) content).getBytes());
            } else if (content instanceof byte[]) {
              writeJarEntry(jarStream, (byte[]) content);
            } else if (content instanceof File) {
              writeJarEntry(jarStream, (File) content);
            }
          }
        }

        jarStream.flush();
        fileStream.getFD().sync();
      } finally {
        if (jarStream != null) {
          try {
            jarStream.close();
          } catch (Exception jarNotClosed) {
            logger.debug("Error closing jar file", jarNotClosed);
          }
        }
        if (fileStream != null) {
          try {
            fileStream.close();
          } catch (Exception fileNotClosed) {
            logger.debug("Error closing file", fileNotClosed);
          }
        }
      }
    }
  }

  private static void writeJarEntry(OutputStream stream, byte[] entry) throws IOException {
    stream.write(entry, 0, entry.length);
  }

  private static void writeJarEntry(OutputStream stream, File entry) throws IOException {
    FileInputStream fileContentStream = null;
    try {
      fileContentStream = new FileInputStream(entry);
      IOUtils.copy(fileContentStream, stream);
    } finally {
      if (fileContentStream != null) {
        try {
          fileContentStream.close();
        } catch (Exception fileContentNotClosed) {
          logger.debug("Error closing file", fileContentNotClosed);
        }
      }
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
