/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.deployment.internal.application.ApplicationClassLoader;
import org.mule.runtime.module.deployment.internal.nativelib.NativeLibraryFinder;

import java.net.URL;
import java.util.List;

public class MuleApplicationClassLoader extends MuleDeployableArtifactClassLoader implements ApplicationClassLoader {

  static {
    registerAsParallelCapable();
  }

  private NativeLibraryFinder nativeLibraryFinder;

  public MuleApplicationClassLoader(ArtifactDescriptor artifactDescriptor, ClassLoader parentCl,
                                    NativeLibraryFinder nativeLibraryFinder, List<URL> urls,
                                    ClassLoaderLookupPolicy lookupPolicy, List<ArtifactClassLoader> artifactPluginClassLoaders) {
    super(artifactDescriptor, urls.toArray(new URL[0]), parentCl, lookupPolicy, artifactPluginClassLoaders);

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
    return new String[] {MuleFoldersUtil.getAppClassesFolder(getArtifactName()).getAbsolutePath()};
  }
}
