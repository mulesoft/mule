/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.application;

import static java.lang.Thread.currentThread;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.deployment.model.api.application.ApplicationClassLoader;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.internal.nativelib.NativeLibraryFinder;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;

import java.net.URL;
import java.util.List;

public class MuleApplicationClassLoader extends MuleDeployableArtifactClassLoader implements ApplicationClassLoader {

  static {
    registerAsParallelCapable();
  }

  private NativeLibraryFinder nativeLibraryFinder;

  public MuleApplicationClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor, ClassLoader parentCl,
                                    NativeLibraryFinder nativeLibraryFinder, List<URL> urls,
                                    ClassLoaderLookupPolicy lookupPolicy, List<ArtifactClassLoader> artifactPluginClassLoaders) {
    super(artifactId, artifactDescriptor, urls.toArray(new URL[0]), parentCl, lookupPolicy, artifactPluginClassLoaders);

    this.nativeLibraryFinder = nativeLibraryFinder;
  }

  @Override
  protected String findLibrary(String name) {
    String libraryPath = super.findLibrary(name);

    libraryPath = nativeLibraryFinder.findLibrary(name, libraryPath);

    return libraryPath;
  }

  @Override
  protected String[] getLocalResourceLocations() {
    // Always the first element corresponds to the application's classes folder
    ClassLoaderModel classLoaderModel = this.<ApplicationDescriptor>getArtifactDescriptor().getClassLoaderModel();
    return new String[] {classLoaderModel.getUrls()[0].getFile()};
  }

  /**
   * Resolves the plugin classloader of the thread context classloader artifact.
   * <p>
   * If that classloader doesn't contain plugins, the current context classloader is returned.
   * 
   * @return the plugin classloader of the current thread context artifact.
   */
  public static List<ClassLoader> resolveContextArtifactPluginClassLoaders() {
    if (currentThread().getContextClassLoader() instanceof MuleApplicationClassLoader) {
      return ((MuleApplicationClassLoader) currentThread().getContextClassLoader()).getArtifactPluginClassLoaders().stream()
          .map(acl -> acl.getClassLoader()).collect(toList());
    } else {
      return singletonList(currentThread().getContextClassLoader());
    }
  }
}
