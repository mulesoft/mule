/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.Integer.toHexString;
import static java.lang.String.format;
import static java.lang.System.identityHashCode;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FilenameUtils.normalize;
import static org.apache.commons.lang3.ClassUtils.getPackageName;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.module.artifact.api.classloader.exception.ClassNotFoundInRegionException;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
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

  protected static final String REGION_OWNER_CANNOT_BE_REMOVED_ERROR = "Region owner cannot be removed";

  static {
    registerAsParallelCapable();
  }

  private static final Logger LOGGER = getLogger(RegionClassLoader.class);
  private static final String RESOURCE_PREFIX = "resource::";
  private static final Pattern GAV_PATTERN = Pattern.compile(RESOURCE_PREFIX + "(\\S+):(\\S+):(\\S+)?:(\\S+)");
  private static final Set<String> API_CLASSIFIERS = newHashSet("raml", "raml-fragment", "oas");

  private final ReadWriteLock innerStateRWLock = new ReentrantReadWriteLock();
  private final Lock innerStateReadLock = innerStateRWLock.readLock();
  private final Lock innerStateWriteLock = innerStateRWLock.writeLock();

  private final List<RegionMemberClassLoader> registeredClassLoaders = new ArrayList<>();
  private final Map<String, ArtifactClassLoader> packageMapping = new HashMap<>();
  private final Map<String, List<ArtifactClassLoader>> resourceMapping = new HashMap<>();
  private final Map<BundleDescriptor, URLClassLoader> descriptorMapping = new HashMap<>();
  private ArtifactClassLoader ownerClassLoader;

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
    return registeredClassLoaders.stream().map(r -> r.unfilteredClassLoader).collect(toList());
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

    innerStateWriteLock.lock();
    try {
      RegionMemberClassLoader registeredClassLoader = findRegisteredClassLoader(artifactClassLoader);
      if (artifactClassLoader == ownerClassLoader || registeredClassLoader != null) {
        throw new IllegalArgumentException(createClassLoaderAlreadyInRegionError(artifactClassLoader.getArtifactId()));
      }

      if (ownerClassLoader == null) {
        ownerClassLoader = artifactClassLoader;
      } else {
        registeredClassLoaders.add(new RegionMemberClassLoader(artifactClassLoader, filter));
      }

      filter.getExportedClassPackages().forEach(p -> {
        LookupStrategy packageLookupStrategy = getClassLoaderLookupPolicy().getPackageLookupStrategy(p);
        if (!(packageLookupStrategy instanceof ChildFirstLookupStrategy)) {
          throw new IllegalStateException(illegalPackageMappingError(p, packageLookupStrategy));
        } else if (packageMapping.containsKey(p)) {
          throw new IllegalStateException(duplicatePackageMappingError(p, packageMapping.get(p), artifactClassLoader));
        } else {
          packageMapping.put(p, artifactClassLoader);
        }
      });

      for (String exportedResource : filter.getExportedResources()) {
        List<ArtifactClassLoader> classLoaders =
            resourceMapping.computeIfAbsent(normalize(exportedResource, true), k -> new ArrayList<>());

        classLoaders.add(artifactClassLoader);
      }
    } finally {
      innerStateWriteLock.unlock();
    }
  }

  static String illegalPackageMappingError(String p, LookupStrategy packageLookupStrategy) {
    return format("Attempt to map package '%s' which was already defined on the region lookup policy with '%s'",
                  p, packageLookupStrategy.getClass().getName());
  }

  static String duplicatePackageMappingError(String packageName, ArtifactClassLoader originalDefinitionClassLoader,
                                             ArtifactClassLoader overridingDefinitionClassLoader) {
    return format("Attempt to redefine mapping for package: '%s'. Original definition classloader is %s, Overriding definition classloader is %s",
                  packageName, originalDefinitionClassLoader.toString(), overridingDefinitionClassLoader.toString());
  }

  private RegionMemberClassLoader findRegisteredClassLoader(ArtifactClassLoader artifactClassLoader) {
    for (RegionMemberClassLoader registeredClassLoader : registeredClassLoaders) {
      if (registeredClassLoader.unfilteredClassLoader == artifactClassLoader) {
        return registeredClassLoader;
      }
    }

    return null;
  }

  /**
   * Removes a class loader member from the region.
   * <p/>
   * Only region members that do not export any package or resoruce can be removed from the region as they are not visible to
   * other members.
   *
   * @param artifactClassLoader class loader to remove. Non null
   * @return true if the class loader is a region member and was removed, false if it is not a region member.
   * @throws IllegalArgumentException if the class loader is the region owner or is a regiion member that exports packages or
   *         resources.
   */
  public boolean removeClassLoader(ArtifactClassLoader artifactClassLoader) {
    checkArgument(artifactClassLoader != null, "artifactClassLoader cannot be null");
    if (ownerClassLoader == artifactClassLoader) {
      throw new IllegalArgumentException(REGION_OWNER_CANNOT_BE_REMOVED_ERROR);
    }

    innerStateWriteLock.lock();
    try {
      RegionMemberClassLoader registeredClassLoader = findRegisteredClassLoader(artifactClassLoader);

      int index = registeredClassLoaders.indexOf(registeredClassLoader);
      if (index < 0) {
        return false;
      }

      if (!registeredClassLoader.filter.getExportedClassPackages().isEmpty()
          || !registeredClassLoader.filter.getExportedResources().isEmpty()) {
        throw new IllegalArgumentException(createCannotRemoveClassLoaderError(artifactClassLoader.getArtifactId()));
      }

      registeredClassLoaders.remove(index);

      return true;
    } finally {
      innerStateWriteLock.unlock();
    }
  }

  @Override
  public Class<?> findLocalClass(String name) throws ClassNotFoundException {
    innerStateReadLock.lock();
    try {
      final String packageName = getPackageName(name);

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
    } finally {
      innerStateReadLock.unlock();
    }
  }

  @Override
  public final URL findResource(final String name) {
    String normalizedName = normalize(name, true);
    // Check exported resources and all matching artifacts
    final List<ArtifactClassLoader> artifactClassLoaders = resourceMapping.get(normalizedName);
    if (artifactClassLoaders != null) {
      for (ArtifactClassLoader artifactClassLoader : artifactClassLoaders) {
        URL url = artifactClassLoader.findResource(normalizedName);
        if (url != null) {
          return url;
        }
      }
    } else if (name.startsWith(RESOURCE_PREFIX)) {
      Matcher matcher = GAV_PATTERN.matcher(name);
      // Check for specific artifact requests
      if (matcher.matches()) {
        String groupId = matcher.group(1);
        String artifactId = matcher.group(2);
        String version = matcher.group(3);
        String resource = matcher.group(4);
        LOGGER.debug("Region request for '{}' in group '{}', artifact '{}' and version '{}'.", resource, groupId, artifactId,
                     version);
        String normalizedResource = normalize(resource, true);

        // Check whether it's an exported resource from a matching artifact
        List<ArtifactClassLoader> exportingArtifactClassLoaders = resourceMapping.get(normalizedResource);
        if (exportingArtifactClassLoaders != null) {
          for (ArtifactClassLoader artifactClassLoader : exportingArtifactClassLoaders) {
            BundleDescriptor descriptor = artifactClassLoader.getArtifactDescriptor().getBundleDescriptor();
            if (isRequestedArtifact(descriptor, groupId, artifactId, version, () -> {
              LOGGER.warn("Required version '{}' for artifact '{}:{}' not found. Searching in available version '{}'...",
                          version, descriptor.getGroupId(), descriptor.getArtifactId(), descriptor.getVersion());
              return true;
            })) {
              return artifactClassLoader.findResource(normalizedResource);
            }
          }
        }

        // Check whether it's a resource from an API dependency, since all those should be considered exported
        ClassLoaderModel classLoaderModel = this.getArtifactDescriptor().getClassLoaderModel();
        for (BundleDependency dependency : classLoaderModel.getDependencies()) {
          Optional<String> classifier = dependency.getDescriptor().getClassifier();
          if (classifier.isPresent() && API_CLASSIFIERS.contains(classifier.get())) {
            BundleDescriptor descriptor = dependency.getDescriptor();
            if (isRequestedArtifact(descriptor, groupId, artifactId, version, () -> false)) {
              return descriptorMapping.computeIfAbsent(descriptor, (CheckedFunction<BundleDescriptor, URLClassLoader>) d -> {
                try {
                  return new URLClassLoader(new URL[] {dependency.getBundleUri().toURL()});
                } catch (MalformedURLException e) {
                  throw new MuleRuntimeException(e);
                }
              }).findResource(normalizedResource);
            }
          }
        }
      }
    }

    return null;
  }

  private boolean isRequestedArtifact(BundleDescriptor descriptor, String groupId, String artifactId, String version,
                                      Supplier<Boolean> onVersionMismatch) {
    boolean versionResult = true;
    if (!descriptor.getVersion().equals(version)) {
      versionResult = onVersionMismatch.get();
    }
    return descriptor.getGroupId().equals(groupId) && descriptor.getArtifactId().equals(artifactId) && versionResult;
  }

  @Override
  public final Enumeration<URL> findResources(final String name) throws IOException {
    String normalizedName = normalize(name, true);
    final List<ArtifactClassLoader> artifactClassLoaders = resourceMapping.get(normalizedName);
    List<Enumeration<URL>> enumerations = new ArrayList<>(registeredClassLoaders.size());

    if (artifactClassLoaders != null) {
      for (ArtifactClassLoader artifactClassLoader : artifactClassLoaders) {

        final Enumeration<URL> partialResources = artifactClassLoader.findResources(normalizedName);
        if (partialResources.hasMoreElements()) {
          enumerations.add(partialResources);
        }
      }
    }

    return new CompoundEnumeration<>(enumerations.toArray(new Enumeration[0]));
  }

  @Override
  public void dispose() {
    registeredClassLoaders.stream().map(c -> c.unfilteredClassLoader).forEach(this::disposeClassLoader);
    registeredClassLoaders.clear();
    descriptorMapping.forEach((descriptor, classloader) -> {
      try {
        classloader.close();
      } catch (IOException e) {
        reportPossibleLeak(e, descriptor.getArtifactId());
      }
    });
    descriptorMapping.clear();
    disposeClassLoader(ownerClassLoader);

    super.dispose();
  }

  private void disposeClassLoader(ArtifactClassLoader classLoader) {
    try {
      classLoader.dispose();
    } catch (Exception e) {
      reportPossibleLeak(e, classLoader.getArtifactId());
    }
  }

  private void reportPossibleLeak(Exception e, String artifactId) {
    final String message = "Error disposing classloader for '{}'. This can cause a memory leak";
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(message, artifactId, e);
    } else {
      LOGGER.error(message, artifactId);
    }
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
    return ownerClassLoader;
  }

  @Override
  public String toString() {
    return format("%s[%s] @%s", getClass().getName(), getArtifactId(), toHexString(identityHashCode(this)));
  }

  static String createCannotRemoveClassLoaderError(String artifactId) {
    return format("Cannot remove classloader '%s' as it exports at least a package or resource", artifactId);
  }

  static String createClassLoaderAlreadyInRegionError(String artifactId) {
    return "Region already contains classloader for artifact:" + artifactId;
  }

  private static class RegionMemberClassLoader {

    final ArtifactClassLoader unfilteredClassLoader;
    final ArtifactClassLoaderFilter filter;

    private RegionMemberClassLoader(ArtifactClassLoader unfilteredClassLoader, ArtifactClassLoaderFilter filter) {
      this.unfilteredClassLoader = unfilteredClassLoader;
      this.filter = filter;
    }
  }
}
