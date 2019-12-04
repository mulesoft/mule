/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.application;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.internal.nativelib.NativeLibraryFinder;
import org.mule.runtime.deployment.model.internal.nativelib.NativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates {@link MuleApplicationClassLoader} instances based on the application descriptor.
 */
public class MuleApplicationClassLoaderFactory implements DeployableArtifactClassLoaderFactory<ApplicationDescriptor> {

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
    final ClassLoaderLookupPolicy classLoaderLookupPolicy = getApplicationClassLoaderLookupPolicy(parent, descriptor);

    return new MuleApplicationClassLoader(artifactId, descriptor, parent.getClassLoader(),
                                          nativeLibraryFinderFactory.create(descriptor.getDataFolderName(),
                                                                            descriptor.getClassLoaderModel().getUrls()),
                                          Arrays.asList(descriptor.getClassLoaderModel().getUrls()),
                                          classLoaderLookupPolicy, artifactPluginClassLoaders);
  }

  private ClassLoaderLookupPolicy getApplicationClassLoaderLookupPolicy(ArtifactClassLoader parent,
                                                                        ApplicationDescriptor descriptor) {

    final Map<String, LookupStrategy> pluginsLookupStrategies = new HashMap<>();

    for (ArtifactPluginDescriptor artifactPluginDescriptor : descriptor.getPlugins()) {
      artifactPluginDescriptor.getClassLoaderModel().getExportedPackages()
          .forEach(p -> pluginsLookupStrategies.put(p, PARENT_FIRST));
    }

    return parent.getClassLoaderLookupPolicy().extend(pluginsLookupStrategies);
  }


}
