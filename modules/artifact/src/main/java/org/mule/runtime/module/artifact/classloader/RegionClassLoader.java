/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

import static java.lang.Integer.toHexString;
import static java.lang.String.format;
import static java.lang.System.identityHashCode;
import static java.util.Collections.emptyList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.module.artifact.classloader.exception.ClassNotFoundInRegionException;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sun.misc.CompoundEnumeration;

/**
 * Defines a classloader for a Mule artifact composed of other artifacts.
 * <p/>
 * Each artifact in the region can provide classes and resources to other artifacts. What is shared is defined on each artifact by
 * providing a {@link ArtifactClassLoaderFilter}. Lookup policy for each classloader added to the region must be aware of any
 * dependency between members of the region and updated accordingly.
 * <p/>
 * For any member X of the region, if it has a dependency against another region member Y, then X must add all the exported
 * packages from Y as PARENT_FIRST. (to indicate that X wants to load those Y's packages)
 * <p/>
 * For any member X of the region, if there is another region member Y that is not a dependency, then X must add all the exported
 * packages from Y as CHILD_ONLY. (to indicate that X does not want to load those Y's packages)
 * <p/>
 * Only a region member can export a given package, but same resources can be exported by many members. The order in which the
 * resources are found will depend on the order in which the class loaders were added to the region.
 */
public class RegionClassLoader extends MuleDeployableArtifactClassLoader {

  static {
    registerAsParallelCapable();
  }

  private final List<ArtifactClassLoader> filteredClassLoaders = new ArrayList<>();
  private final List<ArtifactClassLoader> unfilteredClassLoaders = new ArrayList<>();
  private final Map<String, ArtifactClassLoader> packageMapping = new HashMap<>();
  private final Map<String, List<ArtifactClassLoader>> resourceMapping = new HashMap<>();

  /**
   * Creates a new region.
   *
   * @param artifactId artifact unique ID for the artifact owning the created class loader instance. Non empty.
   * @param artifactDescriptor descriptor for the artifact owning the created class loader instance. Non null.
   * @param parent parent classloader for the region. Non null
   * @param lookupPolicy lookup policy to use on the region
   */
  public RegionClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor, ClassLoader parent,
                           ClassLoaderLookupPolicy lookupPolicy) {
    super(artifactId, artifactDescriptor, new URL[0], parent, lookupPolicy, emptyList());
  }

  @Override
  public List<ArtifactClassLoader> getArtifactPluginClassLoaders() {

    List<ArtifactClassLoader> result = emptyList();
    if (unfilteredClassLoaders.size() > 1) {
      result = unfilteredClassLoaders.subList(1, unfilteredClassLoaders.size());
    }

    return result;
  }

  /**
   * Adds a class loader to the region.
   *
   * @param artifactClassLoader classloader to add. Non null.
   * @param filter filter used to provide access to the added classloader. Non null
   * @throws IllegalArgumentException if the class loader is already a region member.
   */
  public void addClassLoader(ArtifactClassLoader artifactClassLoader, ArtifactClassLoaderFilter filter) {
    checkArgument(artifactClassLoader != null, "artifactClassLoader cannot be null");
    checkArgument(filter != null, "filter cannot be null");
    if (filteredClassLoaders.contains(artifactClassLoader)) {
      throw new IllegalArgumentException("Region already contains classloader " + artifactClassLoader);
    }
    filteredClassLoaders.add(new FilteringArtifactClassLoader(artifactClassLoader, filter));
    unfilteredClassLoaders.add(artifactClassLoader);

    filter.getExportedClassPackages().forEach(p -> packageMapping.put(p, artifactClassLoader));

    for (String exportedResource : filter.getExportedResources()) {
      List<ArtifactClassLoader> classLoaders = resourceMapping.get(exportedResource);

      if (classLoaders == null) {
        classLoaders = new ArrayList<>();
        resourceMapping.put(exportedResource, classLoaders);
      }

      classLoaders.add(artifactClassLoader);
    }
  }

  @Override
  public Class<?> findLocalClass(String name) throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
      final String packageName = ClassUtils.getPackageName(name);

      final ArtifactClassLoader artifactClassLoader = packageMapping.get(packageName);
      if (artifactClassLoader != null) {
        try {
          return artifactClassLoader.findLocalClass(name);
        } catch (ClassNotFoundException e) {
          throw new ClassNotFoundInRegionException(name, getArtifactId(), artifactClassLoader.getArtifactId(), e);
        }
      } else {
        throw new ClassNotFoundInRegionException(name, getArtifactId());
      }
    }
  }

  @Override
  public final URL findResource(final String name) {
    URL resource = null;
    final List<ArtifactClassLoader> artifactClassLoaders = resourceMapping.get(name);
    if (artifactClassLoaders != null) {
      for (ArtifactClassLoader artifactClassLoader : artifactClassLoaders) {
        resource = artifactClassLoader.getClassLoader().getResource(name);
        if (resource != null) {
          break;
        }
      }
    }

    return resource;
  }

  @Override
  public final Enumeration<URL> findResources(final String name) throws IOException {
    final List<ArtifactClassLoader> artifactClassLoaders = resourceMapping.get(name);
    List<Enumeration<URL>> enumerations = new ArrayList<>(filteredClassLoaders.size());

    if (artifactClassLoaders != null) {
      for (ArtifactClassLoader artifactClassLoader : artifactClassLoaders) {

        final Enumeration<URL> partialResources = artifactClassLoader.findResources(name);
        if (partialResources.hasMoreElements()) {
          enumerations.add(partialResources);
        }
      }
    }

    return new CompoundEnumeration<>(enumerations.toArray(new Enumeration[0]));
  }

  @Override
  public void dispose() {
    for (ArtifactClassLoader classLoader : unfilteredClassLoaders) {
      try {
        classLoader.dispose();
      } catch (Exception e) {
        final String message = "Error disposing classloader for '{}'. This can cause a memory leak";
        if (logger.isDebugEnabled()) {
          logger.debug(message, getArtifactDescriptor().getName(), e);
        } else {
          logger.error(message, getArtifactDescriptor().getName());
        }
      }
    }

    unfilteredClassLoaders.clear();
    filteredClassLoaders.clear();

    super.dispose();
  }

  @Override
  public URL findLocalResource(String resourceName) {
    URL resource = getOwnerClassLoader().findLocalResource(resourceName);

    if (resource == null && getParent() instanceof LocalResourceLocator) {
      resource = ((LocalResourceLocator) getParent()).findLocalResource(resourceName);
    }
    return resource;
  }

  private ArtifactClassLoader getOwnerClassLoader() {
    return filteredClassLoaders.get(0);
  }

  @Override
  public String toString() {
    return format("%s[%s] -> %s@%s", getClass().getName(), getArtifactId(), packageMapping.toString(),
                  toHexString(identityHashCode(this)));
  }
}
