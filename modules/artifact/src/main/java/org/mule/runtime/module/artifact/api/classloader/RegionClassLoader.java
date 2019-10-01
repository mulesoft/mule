/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader;

import static java.lang.Integer.toHexString;
import static java.lang.String.format;
import static java.lang.System.identityHashCode;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FilenameUtils.normalize;
import static org.apache.commons.lang3.ClassUtils.getPackageName;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactConstants.API_CLASSIFIERS;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.module.artifact.classloader.ClassLoaderResourceReleaser;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.util.CompoundEnumeration;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;

import org.slf4j.Logger;

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

  private static final String CLASS_EXTENSION = ".class";
  private static final Logger LOGGER = getLogger(RegionClassLoader.class);

  private final ReadWriteLock innerStateRWLock = new ReentrantReadWriteLock();
  private final Lock innerStateReadLock = innerStateRWLock.readLock();
  private final Lock innerStateWriteLock = innerStateRWLock.writeLock();

  private final List<RegionMemberClassLoader> registeredClassLoaders = new ArrayList<>();
  private final Map<String, ArtifactClassLoader> packageMapping = new HashMap<>();
  private final Map<String, List<ArtifactClassLoader>> resourceMapping = new HashMap<>();
  private final Object descriptorMappingLock = new Object();
  private final Map<BundleDescriptor, URLClassLoader> descriptorMapping = new HashMap<>();

  private ArtifactClassLoader ownerClassLoader;

  /**
   * Region specific {@link ResourceReleaser} to add behaviour in the {@link RegionClassLoader#dispose()} execution.
   * By default it will prompt a gc in the JVM if possible to release the softkeys cleared in the caches.
   *
   * This behaviour can be changed by extending {@link RegionClassLoader} and calling the provided protected constructor
   */
  private ResourceReleaser regionResourceReleaser = System::gc;

  /**
   * Creates a new region.
   *
   * @param artifactId artifact unique ID for the artifact owning the created class loader instance. Non empty.
   * @param artifactDescriptor descriptor for the artifact owning the created class loader instance. Non null.
   * @param parent parent classloader for the region. Non null
   * @param lookupPolicy lookup policy to use on the region
   */
  public RegionClassLoader(String artifactId,
                           ArtifactDescriptor artifactDescriptor,
                           ClassLoader parent,
                           ClassLoaderLookupPolicy lookupPolicy) {
    super(artifactId, artifactDescriptor, new URL[0], parent, lookupPolicy, emptyList());
  }

  /**
   * Constructor to be called by extending classes and override the {@link ClassLoaderResourceReleaser} resourceReleaser.
   *
   * @param artifactId artifact unique ID for the artifact owning the created class loader instance. Non empty.
   * @param artifactDescriptor descriptor for the artifact owning the created class loader instance. Non null.
   * @param parent parent classloader for the region. Non null
   * @param lookupPolicy lookup policy to use on the region
   * @param regionResourceReleaser {@link ResourceReleaser} to be called after invocating {@link RegionClassLoader#dispose()}
   * */
  protected RegionClassLoader(String artifactId,
                              ArtifactDescriptor artifactDescriptor,
                              ClassLoader parent,
                              ClassLoaderLookupPolicy lookupPolicy,
                              ResourceReleaser regionResourceReleaser) {
    super(artifactId, artifactDescriptor, new URL[0], parent, lookupPolicy, emptyList());
    this.regionResourceReleaser = regionResourceReleaser;
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

      // *.class files may be requested as resources.
      for (String exportedClassPackage : filter.getExportedClassPackages()) {
        String packageAsDirectory =
            DOT_REPLACEMENT_PATTERN.matcher(exportedClassPackage).replaceAll(PATH_SEPARATOR);
        List<ArtifactClassLoader> classLoaders =
            resourceMapping.computeIfAbsent(packageAsDirectory, k -> new ArrayList<>());
        classLoaders.add(artifactClassLoader);
        classLoaders =
            resourceMapping.computeIfAbsent(packageAsDirectory + PATH_SEPARATOR, k -> new ArrayList<>());
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
      Matcher matcher = GAV_EXTENDED_PATTERN.matcher(name);
      // Check for specific artifact requests
      if (matcher.matches()) {
        String groupId = matcher.group(1);
        String artifactId = matcher.group(2);
        String baseVersion = matcher.group(3);
        String classifier = matcher.group(4);
        String type = matcher.group(5);
        String resource = matcher.group(6);
        LOGGER.debug("Region request for '{}' in group '{}', artifact '{}' and version '{}', with classifier '{}' and type '{}'.",
                     resource, groupId, artifactId, baseVersion, classifier, type);
        String normalizedResource = normalize(resource, true);

        if (API_CLASSIFIERS.contains(classifier) && "zip".equals(type) && !WILDCARD.equals(baseVersion)) {
          // Check whether it's a resource from an API dependency, since all those should be considered exported
          BundleDescriptor requiredDescriptor = new BundleDescriptor.Builder()
              .setGroupId(groupId)
              .setArtifactId(artifactId)
              .setVersion(baseVersion)
              // As the requested version could be an SNAPSHOT we have to compare using baseVersion instead of version
              .setBaseVersion(baseVersion)
              .setClassifier(classifier)
              .setType(type)
              .build();
          URLClassLoader classLoader = descriptorMapping.get(requiredDescriptor);
          if (classLoader != null) {
            return classLoader.findResource(normalizedResource);
          } else {
            ClassLoaderModel classLoaderModel = this.getArtifactDescriptor().getClassLoaderModel();
            for (BundleDependency dependency : classLoaderModel.getDependencies()) {
              BundleDescriptor descriptor = dependency.getDescriptor();
              if (isRequestedArtifact(descriptor, requiredDescriptor, () -> false)) {
                return descriptorMapping.computeIfAbsent(descriptor, (CheckedFunction<BundleDescriptor, URLClassLoader>) d -> {
                  // We don't want class loaders in limbo
                  synchronized (descriptorMappingLock) {
                    if (descriptorMapping.containsKey(descriptor)) {
                      return descriptorMapping.get(descriptor);
                    } else {
                      try {
                        return new URLClassLoader(new URL[] {dependency.getBundleUri().toURL()}, getSystemClassLoader(),
                                                  new NonCachingURLStreamHandlerFactory());
                      } catch (MalformedURLException e) {
                        throw new MuleRuntimeException(e);
                      }
                    }
                  }
                }).findResource(normalizedResource);
              }
            }
          }
        } else {
          // Check whether it's an exported resource from a matching artifact
          List<ArtifactClassLoader> exportingArtifactClassLoaders = resourceMapping.get(normalizedResource);
          if (exportingArtifactClassLoaders != null) {
            for (ArtifactClassLoader artifactClassLoader : exportingArtifactClassLoaders) {
              BundleDescriptor descriptor = artifactClassLoader.getArtifactDescriptor().getBundleDescriptor();
              // The descriptor may not be present during some tests
              if (descriptor != null
                  && isRequestedArtifact(descriptor, groupId, artifactId, baseVersion, ofNullable(classifier), type, () -> {
                    LOGGER.warn("Required version '{}' for artifact '{}:{}' not found. Searching in available version '{}'...",
                                baseVersion, descriptor.getGroupId(), descriptor.getArtifactId(), descriptor.getVersion());
                    return true;
                  })) {
                return artifactClassLoader.findResource(normalizedResource);
              }
            }
          }
        }
      }
    } else if (name.endsWith(CLASS_EXTENSION)) {
      // This is when a class is requested as a resource like with spring classpath scanning.
      int lastIndexOfPackageSeparator = name.lastIndexOf(PATH_SEPARATOR);
      String resourceFolder = name.substring(0, lastIndexOfPackageSeparator != -1 ? lastIndexOfPackageSeparator : 0);
      List<ArtifactClassLoader> resourceFolderArtifactClassLoaders = resourceMapping.get(resourceFolder);
      if (resourceFolderArtifactClassLoaders == null) {
        return null;
      }
      for (ArtifactClassLoader resourceFolderArtifactClassLoader : resourceFolderArtifactClassLoaders) {
        URL url = resourceFolderArtifactClassLoader.findResource(normalizedName);
        if (url != null) {
          return url;
        }
      }
    }

    return null;
  }

  @Override
  public final Enumeration<URL> findResources(final String name) throws IOException {
    String normalizedName = normalize(name, true);
    List<Enumeration<URL>> enumerations = new ArrayList<>(registeredClassLoaders.size());
    if (normalizedName.endsWith("/")) {
      List<Map.Entry<String, List<ArtifactClassLoader>>> entries = resourceMapping.entrySet()
          .stream()
          .filter(entry -> entry.getKey().startsWith(name))
          .collect(toList());
      for (Map.Entry<String, List<ArtifactClassLoader>> entry : entries) {
        List<ArtifactClassLoader> artifactClassLoaders = entry.getValue();
        for (ArtifactClassLoader artifactClassLoader : artifactClassLoaders) {
          enumerations.add(artifactClassLoader.findResources(name));
        }
      }
    } else {
      final List<ArtifactClassLoader> artifactClassLoaders = resourceMapping.get(normalizedName);
      if (artifactClassLoaders != null) {
        for (ArtifactClassLoader artifactClassLoader : artifactClassLoaders) {

          final Enumeration<URL> partialResources = artifactClassLoader.findResources(normalizedName);
          if (partialResources.hasMoreElements()) {
            enumerations.add(partialResources);
          }
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

    //System.gc() by default
    regionResourceReleaser.release();
  }

  private void disposeClassLoader(ArtifactClassLoader classLoader) {
    try {
      classLoader.dispose();
    } catch (Exception e) {
      reportPossibleLeak(e, classLoader.getArtifactId());
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

  public ArtifactClassLoader getOwnerClassLoader() {
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
