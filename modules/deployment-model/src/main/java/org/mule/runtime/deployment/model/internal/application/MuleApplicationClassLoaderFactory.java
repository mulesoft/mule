/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.application;

import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.internal.nativelib.NativeLibraryFinder;
import org.mule.runtime.deployment.model.internal.nativelib.NativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;

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

  /**
   * Creates a new factory
   *
   * @param nativeLibraryFinderFactory creates {@link NativeLibraryFinder} for the created applications. Non null
   */
  public MuleApplicationClassLoaderFactory(NativeLibraryFinderFactory nativeLibraryFinderFactory) {

    checkArgument(nativeLibraryFinderFactory != null, "nativeLibraryFinderFactory cannot be null");
    this.nativeLibraryFinderFactory = nativeLibraryFinderFactory;
  }

  @Override
  public ArtifactClassLoader create(String artifactId, ArtifactClassLoader parent, ApplicationDescriptor descriptor,
                                    List<ArtifactClassLoader> artifactPluginClassLoaders) {
    List<URL> urls = getApplicationResourceUrls(descriptor);

    final ClassLoaderLookupPolicy classLoaderLookupPolicy = getApplicationClassLoaderLookupPolicy(parent, descriptor);

    return new MuleApplicationClassLoader(artifactId, descriptor, parent.getClassLoader(),
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
      urls.add(descriptor.getClassesFolder().toURI().toURL());

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
