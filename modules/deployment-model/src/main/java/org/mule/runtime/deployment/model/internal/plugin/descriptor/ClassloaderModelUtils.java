/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.descriptor;

import static java.lang.String.format;
import org.mule.runtime.module.artifact.net.MulePluginURLConnection;
import org.mule.runtime.deployment.model.api.plugin.classloadermodel.MalformedClassloaderModelException;
import org.mule.runtime.module.artifact.net.MulePluginUrlStreamHandler;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Helper class to introspect elements for any type of classloader, either zip of folder. If the plugin is zipped, it
 * relies heavily on the {@link MulePluginUrlStreamHandler#PROTOCOL} for defining how to expose those URLs. For more
 * information see {@link MulePluginURLConnection} class.
 *
 * @since 4.0
 */
public class ClassloaderModelUtils {

  /**
   * Takes a plugin's location and generates the /classes URL for the future classloader to load it. For the moment it
   * will never be absent due to the fact that if the /classes does not exists, the classloader will take care on its
   * own disregarding it.
   *
   * @param pluginLocation the location of the plugin
   * @return an URL targeting /classes if exist, empty otherwise.
   * @throws MalformedClassloaderModelException
     */
  public static Optional<URL> parseRuntimeClasses(URL pluginLocation) throws MalformedClassloaderModelException {
    try {
      return Optional.of(isZipPlugin(pluginLocation) ? assembleFor(pluginLocation, "classes")
          : new URL(pluginLocation, "classes"));
    } catch (MalformedURLException e) {
      throw new MalformedClassloaderModelException("Cannot assembly /classes URL", e);
    }
  }

  /**
   * Takes a plugin's pluginLocation and generates as many URLs as jars within the /lib folder are.
   * @param pluginLocation the pluginLocation of the plugin
   * @return an array of URLs targeting the .jar files inside of /lib for the current plugin.
   * @throws MalformedClassloaderModelException
     */
  public static URL[] parseRuntimeLibs(URL pluginLocation) throws MalformedClassloaderModelException {
    if (isZipPlugin(pluginLocation)) {
      return parseZipRuntimeLibs(pluginLocation);
    } else {
      return parseFolderRuntimeLibs(pluginLocation);
    }

  }

  private static boolean isZipPlugin(URL pluginLocation) {
    return pluginLocation.getFile().endsWith(".zip");
  }

  static private URL assembleFor(URL pluginLocation, String resource) throws MalformedURLException {
    return new URL(MulePluginUrlStreamHandler.PROTOCOL + ":" + pluginLocation + "!/" + resource + "!/");
  }

  private static URL[] parseZipRuntimeLibs(URL pluginLocation) throws MalformedClassloaderModelException {
    try {
      List<URL> urls = new ArrayList<>();
      ZipInputStream zipInputStream = new ZipInputStream(pluginLocation.openStream());
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (entry.getName().startsWith("lib/") && entry.getName().endsWith(".jar")) {
          urls.add(assembleFor(pluginLocation, entry.getName()));
        }
      }
      return urls.toArray(new URL[urls.size()]);
    } catch (IOException e) {
      throw new MalformedClassloaderModelException(format("There was a problem while unziping [%s]",
                                                          pluginLocation.toString()),
                                                   e);
    }

  }

  private static URL[] parseFolderRuntimeLibs(URL pluginLocation) throws MalformedClassloaderModelException {
    File pluginFolder;
    try {
      pluginFolder = new File(pluginLocation.toURI());
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(format("There was an issue trying to create a file for %s URL",
                                                pluginLocation.toString()),
                                         e);
    }
    List<File> files = new ArrayList<>();
    final File libDir = new File(pluginFolder, "lib");
    if (libDir.exists()) {
      files.addAll(Arrays.asList(libDir.listFiles(pathname -> pathname.getName().endsWith(".jar"))));
    }

    URL[] urls = new URL[files.size()];
    for (int i = 0; i < files.size(); i++) {
      try {
        urls[i] = files.get(i).toURI().toURL();
      } catch (MalformedURLException e) {
        throw new MalformedClassloaderModelException("Failed to create the URL",
                                                     e);
      }
    }
    return urls;
  }
}
