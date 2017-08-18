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
import static org.apache.commons.io.FileUtils.toFile;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.deployment.model.api.application.ApplicationClassLoader;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.internal.nativelib.NativeLibraryFinder;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
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
    return new String[] {toFile(classLoaderModel.getUrls()[0]).getPath()};
  }

  /**
   * Resolves the plugin classloader of the thread context classloader artifact.
   * <p>
   * If that classloader doesn't contain plugins, the current context classloader is returned.
   * 
   * @return the plugin classloader of the current thread context artifact.
   */
  public static List<ClassLoader> resolveContextArtifactPluginClassLoaders() {
    // TODO MULE-12254
    // When running the tests, the classloader hierarchy is build with the launcher, but when executing here we are with the
    // container.
    // This is why this reflection bloat is required.

    final Method getArtifactPluginClassLoaders;
    try {
      getArtifactPluginClassLoaders =
          currentThread().getContextClassLoader().getClass().getMethod("getArtifactPluginClassLoaders");
    } catch (NoSuchMethodException | SecurityException e) {
      return singletonList(currentThread().getContextClassLoader());
    }

    final List artifactPluginClassLoaders;
    try {
      artifactPluginClassLoaders = (List) getArtifactPluginClassLoaders.invoke(currentThread().getContextClassLoader());
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new MuleRuntimeException(e);
    }

    final List<ClassLoader> classLoaders =
        new ArrayList<ClassLoader>((List<ClassLoader>) artifactPluginClassLoaders.stream().map(acl -> {
          try {
            return acl.getClass().getMethod("getClassLoader").invoke(acl);
          } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
              | SecurityException e) {
            throw new MuleRuntimeException(e);
          }
        }).collect(toList()));

    return classLoaders;
  }
}
