/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.application;

import static org.mule.runtime.module.artifact.api.classloader.BlockingLoggerResolutionClassRegistry.getBlockingLoggerResolutionClassRegistry;

import static java.lang.Thread.currentThread;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import static org.apache.commons.io.FileUtils.toFile;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.deployment.model.api.application.ApplicationClassLoader;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.internal.nativelib.NativeLibraryFinder;
import org.mule.runtime.deployment.model.internal.nativelib.NativeLibraryLoaderMuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MuleApplicationClassLoader extends NativeLibraryLoaderMuleDeployableArtifactClassLoader
    implements ApplicationClassLoader {

  static {
    registerAsParallelCapable();
    getBlockingLoggerResolutionClassRegistry().registerClassNeedingBlockingLoggerResolution(MuleApplicationClassLoader.class);
  }

  private NativeLibraryFinder nativeLibraryFinder;

  public MuleApplicationClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor, ClassLoader parentCl,
                                    NativeLibraryFinder nativeLibraryFinder, List<URL> urls,
                                    ClassLoaderLookupPolicy lookupPolicy, List<ArtifactClassLoader> artifactPluginClassLoaders) {
    super(artifactId, artifactDescriptor, parentCl, nativeLibraryFinder, urls, lookupPolicy, artifactPluginClassLoaders);
    this.nativeLibraryFinder = nativeLibraryFinder;
  }

  @Override
  protected String findLibrary(String name) {
    if (supportNativeLibraryDependencies) {
      loadNativeLibraryDependencies(name);
    }

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
   * Collects all the plugin classloaders that are available in the deployable artifact classloader hierarchy.
   * <p/>
   * For every class loader in the artifact class loader hierarchy, if that classloader does not contain plugins, the same
   * classloader is added to the list.
   *
   * @return a {@link List<ClassLoader>} containing all the plugin class loaders in the artifact classloader hierarchy.
   */
  public static List<ClassLoader> resolveContextArtifactPluginClassLoaders() {
    Set<ClassLoader> resolvedClassLoaders = new HashSet<>();
    ClassLoader originalClassLoader = currentThread().getContextClassLoader();

    resolvedClassLoaders.addAll(resolveContextArtifactPluginClassLoadersForCurrentClassLoader());

    ClassLoader tmpClassLoader = originalClassLoader;
    while (tmpClassLoader.getParent() != null
        && MuleDeployableArtifactClassLoader.class.isAssignableFrom(tmpClassLoader.getParent().getClass())) {
      try {
        tmpClassLoader = tmpClassLoader.getParent();
        currentThread().setContextClassLoader(tmpClassLoader);
        resolvedClassLoaders.addAll(resolveContextArtifactPluginClassLoadersForCurrentClassLoader());
      } finally {
        currentThread().setContextClassLoader(originalClassLoader);
      }
    }

    return new ArrayList<>(resolvedClassLoaders);
  }

  /**
   * Resolves the plugin classloader of the thread context classloader artifact.
   * <p>
   * If that classloader doesn't contain plugins, the current context classloader is returned.
   * 
   * @return the plugin classloader of the current thread context artifact.
   */
  private static List<ClassLoader> resolveContextArtifactPluginClassLoadersForCurrentClassLoader() {
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
