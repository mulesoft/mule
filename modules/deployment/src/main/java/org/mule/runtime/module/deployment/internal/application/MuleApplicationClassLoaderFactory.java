/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.application;

import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppClassesFolder;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.deployment.internal.MuleApplicationClassLoader;
import org.mule.runtime.module.deployment.internal.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.deployment.internal.nativelib.NativeLibraryFinderFactory;
import org.mule.runtime.module.deployment.internal.plugin.ArtifactPluginDescriptor;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates {@link MuleApplicationClassLoader} instances based on the application descriptor.
 */
public class MuleApplicationClassLoaderFactory implements DeployableArtifactClassLoaderFactory<ApplicationDescriptor> {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final NativeLibraryFinderFactory nativeLibraryFinderFactory;

  public MuleApplicationClassLoaderFactory(NativeLibraryFinderFactory nativeLibraryFinderFactory) {
    this.nativeLibraryFinderFactory = nativeLibraryFinderFactory;
  }

  @Override
  public ArtifactClassLoader create(ArtifactClassLoader parent, ApplicationDescriptor descriptor,
                                    List<ArtifactClassLoader> artifactPluginClassLoaders) {
    List<URL> urls = getApplicationResourceUrls(descriptor);

    final ClassLoaderLookupPolicy classLoaderLookupPolicy = getApplicationClassLoaderLookupPolicy(parent, descriptor);

    return new MuleApplicationClassLoader(descriptor, parent.getClassLoader(),
                                          nativeLibraryFinderFactory.create(descriptor.getName()), urls,
                                          classLoaderLookupPolicy, artifactPluginClassLoaders);
  }

  private ClassLoaderLookupPolicy getApplicationClassLoaderLookupPolicy(ArtifactClassLoader parent,
                                                                        ApplicationDescriptor descriptor) {
    final Map<String, ClassLoaderLookupStrategy> pluginsLookupStrategies = new HashMap<>();

    for (ArtifactPluginDescriptor artifactPluginDescriptor : descriptor.getPlugins()) {
      artifactPluginDescriptor.getClassLoaderFilter().getExportedClassPackages()
          .forEach(p -> pluginsLookupStrategies.put(p, PARENT_FIRST));
    }

    return parent.getClassLoaderLookupPolicy().extend(pluginsLookupStrategies);
  }

  private List<URL> getApplicationResourceUrls(ApplicationDescriptor descriptor) {
    List<URL> urls = new LinkedList<>();
    try {
      urls.add(getAppClassesFolder(descriptor.getName()).toURI().toURL());

      for (URL url : descriptor.getRuntimeLibs()) {
        urls.add(url);
      }

      for (URL url : descriptor.getSharedRuntimeLibs()) {
        urls.add(url);
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to create classloader for application", e);
    }

    if (!urls.isEmpty() && logger.isInfoEnabled()) {
      logArtifactRuntimeUrls(descriptor, urls);
    }

    return urls;
  }

  private void logArtifactRuntimeUrls(ApplicationDescriptor descriptor, List<URL> urls) {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("[%s] Loading the following jars:%n", descriptor.getName()));
    sb.append("=============================").append(LINE_SEPARATOR);

    for (URL url : urls) {
      sb.append(url).append(LINE_SEPARATOR);
    }

    sb.append("=============================").append(LINE_SEPARATOR);
    logger.info(sb.toString());
  }
}
