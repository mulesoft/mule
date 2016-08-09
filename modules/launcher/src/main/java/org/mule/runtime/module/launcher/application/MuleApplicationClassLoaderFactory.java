/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.application;

import static org.apache.commons.io.FileUtils.listFiles;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getAppClassesFolder;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getAppLibFolder;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getMulePerAppLibFolder;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.launcher.MuleApplicationClassLoader;
import org.mule.runtime.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.launcher.nativelib.NativeLibraryFinderFactory;
import org.mule.runtime.core.util.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates {@link MuleApplicationClassLoader} instances based on the application descriptor.
 */
public class MuleApplicationClassLoaderFactory implements DeployableArtifactClassLoaderFactory<ApplicationDescriptor> {

  public static final String CLASS_EXTENSION = ".class";

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final NativeLibraryFinderFactory nativeLibraryFinderFactory;

  public MuleApplicationClassLoaderFactory(NativeLibraryFinderFactory nativeLibraryFinderFactory) {
    this.nativeLibraryFinderFactory = nativeLibraryFinderFactory;
  }

  @Override
  public ArtifactClassLoader create(ArtifactClassLoader parent, ApplicationDescriptor descriptor,
                                    List<ArtifactClassLoader> artifactPluginClassLoders) {
    List<URL> urls = getApplicationResourceUrls(descriptor);

    return new MuleApplicationClassLoader(descriptor.getName(), parent.getClassLoader(),
                                          nativeLibraryFinderFactory.create(descriptor.getName()), urls,
                                          parent.getClassLoaderLookupPolicy(), artifactPluginClassLoders);
  }

  private List<URL> getApplicationResourceUrls(ApplicationDescriptor descriptor) {
    List<URL> urls = new LinkedList<>();
    try {
      urls.add(getAppClassesFolder(descriptor.getName()).toURI().toURL());
      urls.addAll(findJars(descriptor.getName(), getAppLibFolder(descriptor.getName()), true));
      urls.addAll(findJars(descriptor.getName(), getMulePerAppLibFolder(), true));
    } catch (IOException e) {
      throw new RuntimeException("Unable to create classloader for application", e);
    }

    return urls;
  }

  /**
   * Add jars from the supplied directory to the class path
   */
  private List<URL> findJars(String appName, File dir, boolean verbose) throws MalformedURLException {
    List<URL> result = new LinkedList<>();

    if (dir.exists() && dir.canRead()) {
      @SuppressWarnings("unchecked")
      Collection<File> jars = listFiles(dir, new String[] {"jar"}, false);

      if (!jars.isEmpty() && logger.isInfoEnabled()) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s] Loading the following jars:%n", appName));
        sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);

        for (File jar : jars) {
          sb.append(jar.toURI().toURL()).append(SystemUtils.LINE_SEPARATOR);
        }

        sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);

        if (verbose) {
          logger.info(sb.toString());
        } else {
          logger.debug(sb.toString());
        }
      }

      for (File jar : jars) {
        result.add(jar.toURI().toURL());
      }
    }

    return result;
  }
}
