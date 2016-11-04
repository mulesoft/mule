/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.descriptor.resource;

import static java.lang.String.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Util to load resources from a plugin file URL, whether is a folder or a zip file
 *
 * @since 4.0
 */
public class URLPluginResourceLoader {

  public Optional<InputStream> loadResource(URL location, String resource) {

    File pluginFile;
    try {
      pluginFile = new File(location.toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(format("There was an issue while assembling a file with location %s",
                                        location.toString()),
                                 e);
    }
    if (pluginFile.getName().endsWith(".zip")) {
      return loadResourceFromZip(pluginFile, resource);
    } else if (pluginFile.exists() && pluginFile.isDirectory()) {
      return loadFromFolder(pluginFile, resource);
    }
    throw new RuntimeException(format("There was an issue while assembling a file with location %s",
                                      location.toString()));
  }

  private Optional<InputStream> loadResourceFromZip(File pluginFile, String resource) {
    Optional<InputStream> inputStream;

    ZipFile zipFile = getZipFile(pluginFile);
    ZipEntry descriptorEntry = zipFile.getEntry(resource);
    if (descriptorEntry != null) {
      try {
        inputStream = Optional.of(zipFile.getInputStream(descriptorEntry));
      } catch (IOException e) {
        throw new IllegalArgumentException("There was an issue while opening the stream for the resource", e);
      }
    } else {
      inputStream = Optional.empty();
    }
    return inputStream;
  }

  private ZipFile getZipFile(File pluginFile) {
    try {
      return new ZipFile(pluginFile);
    } catch (IOException e) {
      throw new IllegalArgumentException(format("Can't load zip file from [%s]", pluginFile.getAbsolutePath()), e);
    }
  }

  private Optional<InputStream> loadFromFolder(File pluginFile, String resource) {
    Optional<InputStream> jsonStream;
    File file = new File(pluginFile, resource);
    if (file.exists()) {
      try {
        jsonStream = Optional.of(new FileInputStream(file));
      } catch (FileNotFoundException e) {
        throw new IllegalArgumentException("There was an issue while opening the stream for the resource", e);
      }
    } else {
      jsonStream = Optional.empty();
    }
    return jsonStream;
  }
}
