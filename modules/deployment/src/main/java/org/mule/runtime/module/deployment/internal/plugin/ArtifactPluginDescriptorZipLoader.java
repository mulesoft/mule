/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.plugin;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.mule.module.artifact.classloader.net.MuleArtifactUrlConnection.CLASSES_FOLDER;
import static org.mule.module.artifact.classloader.net.MuleArtifactUrlConnection.SEPARATOR;
import static org.mule.module.artifact.classloader.net.MuleArtifactUrlStreamHandler.PROTOCOL;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Extracts the needed information of a mule plugin when its current format is a ZIP file.
 *
 * @since 4.0
 */
class ArtifactPluginDescriptorZipLoader extends ArtifactPluginDescriptorLoader {

  static final String EXTENSION_ZIP = ".zip";

  private final ZipFile pluginZip;

  ArtifactPluginDescriptorZipLoader(File pluginLocation) {
    super(pluginLocation);
    try {
      this.pluginZip = new ZipFile(pluginLocation);
    } catch (IOException e) {
      throw new ArtifactDescriptorCreateException(format("There was an issue opening the ZIP file at '%s'",
                                                         pluginLocation.getAbsolutePath()),
                                                  e);
    }
  }

  @Override
  protected String getName() {
    return removeEnd(pluginLocation.getName(), EXTENSION_ZIP);
  }

  @Override
  protected URL getClassesUrl() throws MalformedURLException {
    return assembleFor(pluginLocation, CLASSES_FOLDER);
  }

  @Override
  protected List<URL> getRuntimeLibs() throws MalformedURLException {
    List<URL> urls = new ArrayList<>();
    try {
      Enumeration<? extends ZipEntry> entries = pluginZip.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (entry.getName().startsWith("lib/") && entry.getName().endsWith(".jar")) {
          urls.add(assembleFor(pluginLocation, entry.getName()));
        }
      }
    } catch (IOException e) {
      throw new ArtifactDescriptorCreateException(format("There was a problem while unzipping [%s]",
                                                         pluginLocation.toString()),
                                                  e);
    }
    return urls;
  }

  @Override
  protected Optional<InputStream> loadResourceFrom(String resource) {
    Optional<InputStream> inputStream;
    ZipEntry descriptorEntry = pluginZip.getEntry(resource);
    if (descriptorEntry != null) {
      try {
        inputStream = Optional.of(pluginZip.getInputStream(descriptorEntry));
      } catch (IOException e) {
        throw new ArtifactDescriptorCreateException(format("Cannot read resource '%s' from ZIP '%s'", resource,
                                                           pluginZip.getName()),
                                                    e);
      }
    } else {
      inputStream = empty();
    }
    return inputStream;
  }

  @Override
  protected void close() {
    try {
      pluginZip.close();
    } catch (IOException e) {
      throw new ArtifactDescriptorCreateException(format("There was a problem closing the ZIP file '%s'", pluginZip.getName()));
    }
  }

  private URL assembleFor(File pluginLocation, String resource) throws MalformedURLException {
    return new URL(PROTOCOL + ":" + pluginLocation.toURI() + SEPARATOR + resource + SEPARATOR);
  }
}
