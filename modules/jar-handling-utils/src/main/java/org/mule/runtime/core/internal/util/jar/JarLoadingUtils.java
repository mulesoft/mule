/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

/**
 * This utility class is responsible for loading jars and also creating jar connection. The implementation of load methods is
 * different between Java 8 and Java 11+ as Java 8 we need to access JDK internal package sun.net.www.protocol.jar. Hence this
 * class will have 2 implementations and this module will be a MRJAR
 *
 * @since 4.5
 */
public class JarLoadingUtils {

  private JarLoadingUtils() {
    // utility class only
  }

  /**
   * Returns a JarURLConnection used in Mule Java 11+ runtime
   *
   * @return a JarURLConnection
   *
   * @throws MalformedURLException in case the schemaURI is malformed
   * @throws IOException           an IO exception during the jar connection creation
   */
  public static JarURLConnection getJarConnection(URL possibleUrl) throws MalformedURLException, IOException {
    return (JarURLConnection) possibleUrl.openConnection();
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
    JarURLConnection jarConnection = (JarURLConnection) jsonDescriptorUrl.openConnection();
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
    JarURLConnection jarConnection = (JarURLConnection) jarFile.openConnection();
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
   * @param jarFile  the jar filejava -version
   * @param filePath the path within the jar file
   * @return an URL to the {@code filePath} within the {@code jarFile}
   * @throws MalformedURLException if the provided {@code filePath} is malformed
   */
  public static URL getUrlWithinJar(File jarFile, String filePath) throws MalformedURLException {
    return new URL("jar:" + jarFile.toURI() + "!/" + filePath);
  }
}
