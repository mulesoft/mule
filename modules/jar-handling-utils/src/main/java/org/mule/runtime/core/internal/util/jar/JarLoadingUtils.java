/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.jar;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.apache.commons.io.IOUtils.toByteArray;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class JarLoadingUtils {

  private static final Logger logger = LoggerFactory.getLogger(JarLoadingUtils.class);

  private JarLoadingUtils() {
    // utility class only
  }

  public static JarURLConnection getJarConnection(URL possibleUrl) throws MalformedURLException, IOException {
    JarURLConnection jarConnection =
        new sun.net.www.protocol.jar.JarURLConnection(possibleUrl, new sun.net.www.protocol.jar.Handler());
    return jarConnection;
  }

  /**
   * Creates an URL to a path within a jar file.
   *
   * @param jarFile  the jar filejava -version
   * @param filePath the path within the jar file
   * @return an URL to the {@code filePath} within the {@code jarFile}
   * @throws MalformedURLException if the provided {@code filePath} is malformed
   */
  public static URL getUrlWithinJar(File jarFile, String filePath) throws MalformedURLException {
    return new URL("jar:" + jarFile.toURI().toString() + "!/" + filePath);
  }

  /**
   * Loads the content of a file within a jar into a byte array.
   *
   * @param jarFile  the jar file
   * @param filePath the path to the file within the jar file
   * @return the content of the file as byte array or empty if the file does not exists within the jar file.
   * @throws IOException if there was a problem reading from the jar file.
   */
  public static Optional<byte[]> loadFileContentFrom(File jarFile, String filePath) throws IOException {
    URL jsonDescriptorUrl = getUrlWithinJar(jarFile, filePath);
    /*
     * A specific implementation of JarURLConnection is required to read jar content because not all implementations support ways
     * to disable connection caching. Disabling connection caching is necessary to avoid file descriptor leaks.
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
     * A specific implementation of JarURLConnection is required to read jar content because not all implementations support ways
     * to disable connection caching. Disabling connection caching is necessary to avoid file descriptor leaks.
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

}
