/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.plugin;

import static java.lang.String.format;
import static java.util.Optional.empty;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.filefilter.SuffixFileFilter;

/**
 * Extracts the needed information of a mule plugin when its current format is a folder.
 *
 * @since 4.0
 */
class ArtifactPluginDescriptorFolderLoader extends ArtifactPluginDescriptorLoader {

  ArtifactPluginDescriptorFolderLoader(File pluginLocation) {
    super(pluginLocation);
  }

  @Override
  protected String getName() {
    return pluginLocation.getName();
  }

  @Override
  protected URL getClassesUrl() throws MalformedURLException {
    return new File(pluginLocation, "classes").toURI().toURL();
  }

  @Override
  protected List<URL> getRuntimeLibs() throws MalformedURLException {
    List<URL> urls = new ArrayList<>();
    final File libDir = new File(pluginLocation, "lib");
    if (libDir.exists()) {
      final File[] jars = libDir.listFiles((FilenameFilter) new SuffixFileFilter(".jar"));
      for (File jar : jars) {
        urls.add(jar.toURI().toURL());
      }
    }
    return urls;
  }

  @Override
  protected Optional<InputStream> loadResourceFrom(String resource) {
    Optional<InputStream> inputStream;
    final File file = new File(pluginLocation, resource);
    if (file.exists()) {
      try {
        inputStream = Optional.of(new FileInputStream(file));
      } catch (FileNotFoundException e) {
        throw new ArtifactDescriptorCreateException(format("Cannot read resource '%s' from file '%s'", resource,
                                                           pluginLocation.getAbsolutePath()),
                                                    e);
      }
    } else {
      inputStream = empty();
    }
    return inputStream;
  }
}
