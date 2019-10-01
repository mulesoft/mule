/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.descriptor;

import static java.lang.Boolean.FALSE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static org.apache.commons.io.FilenameUtils.separatorsToUnix;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains all the information needed to create a {@link ClassLoader} for a Mule artifact.
 */
public final class ClassLoaderModel {

  /**
   * Defines a {@link ClassLoaderModel} with empty configuration
   */
  public static final ClassLoaderModel NULL_CLASSLOADER_MODEL =
      new ClassLoaderModel(new URL[0], emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), false);

  private final URL[] urls;
  private final Set<String> exportedPackages;
  private final Set<String> exportedResources;
  private final Set<String> localPackages;
  private final Set<String> localResources;
  private final Set<BundleDependency> dependencies;
  private final Set<String> privilegedExportedPackages;
  private final Set<String> privilegedArtifacts;
  private final boolean includeTestDependencies;

  private ClassLoaderModel(URL[] urls, Set<String> exportedPackages, Set<String> exportedResources,
                           Set<String> localPackages, Set<String> localResources,
                           Set<BundleDependency> dependencies, Set<String> privilegedExportedPackages,
                           Set<String> privilegedArtifacts, boolean includeTestDependencies) {
    this.urls = urls;
    this.exportedPackages = exportedPackages;
    this.exportedResources = exportedResources;
    this.localPackages = localPackages;
    this.localResources = localResources;
    this.dependencies = dependencies;
    this.privilegedExportedPackages = privilegedExportedPackages;
    this.privilegedArtifacts = privilegedArtifacts;
    this.includeTestDependencies = includeTestDependencies;
  }

  /**
   * @return the URLs to use on the {@link ClassLoader}. Non null
   */
  public URL[] getUrls() {
    return urls;
  }

  /**
   * @return the class packages to be exported on the {@link ClassLoader}. Non null
   */
  public Set<String> getExportedPackages() {
    return exportedPackages;
  }

  /**
   * @return the resources to be exported on the {@link ClassLoader}. Non null
   */
  public Set<String> getExportedResources() {
    return exportedResources;
  }

  /**
   * @return the Java packages to be loaded from the artifact itself, even if some other artifact in the region exports it. Non
   *         null
   */
  public Set<String> getLocalPackages() {
    return localPackages;
  }

  /**
   * @return the resources to be loaded from the artifact itself, even if some other artifact in the region exports it. Non null
   */
  public Set<String> getLocalResources() {
    return localResources;
  }

  /**
   * @return the artifact dependencies required to create the {@link ClassLoader}. Non null
   */
  public Set<BundleDependency> getDependencies() {
    return dependencies;
  }

  /**
   * @return the Java package names to be exported as privileged API on the {@link ClassLoader}. Non null
   */
  public Set<String> getPrivilegedExportedPackages() {
    return privilegedExportedPackages;
  }

  /**
   * @return the artifact IDs that have access to the privileged API defined on the {@link ClassLoader}. Each artifact is defined using Maven's groupId:artifactId. Non null
   */
  public Set<String> getPrivilegedArtifacts() {
    return privilegedArtifacts;
  }

  /**
   * @return {@code true} if the model should include {@code test} scope dependencies when resolving the class loader {@link URL urls} for the artifact.
   */
  public boolean isIncludeTestDependencies() {
    return includeTestDependencies;
  }

  /**
   * Builds a {@link ClassLoaderModel}
   */
  public static class ClassLoaderModelBuilder {

    private final Set<String> packages = new HashSet<>();
    private final Set<String> resources = new HashSet<>();
    private final Set<String> localPackages = new HashSet<>();
    private final Set<String> localResources = new HashSet<>();
    private List<URL> urls = new ArrayList<>();
    protected Set<BundleDependency> dependencies = new HashSet<>();
    private final Set<String> privilegedExportedPackages = new HashSet<>();
    private final Set<String> privilegedArtifacts = new HashSet<>();
    private Boolean includeTestDependencies = FALSE;

    /**
     * Creates an empty builder.
     */
    public ClassLoaderModelBuilder() {}

    /**
     * Creates a builder initialized with a {@link ClassLoaderModel}'s state
     *
     * @param source used to initialize the created object. Non null.
     */
    public ClassLoaderModelBuilder(ClassLoaderModel source) {
      checkArgument(source != null, "source cannot be null");

      this.packages.addAll(source.exportedPackages);
      this.resources.addAll(source.exportedResources);
      this.localPackages.addAll(source.localPackages);
      this.localResources.addAll(source.localResources);
      this.urls = new ArrayList<>(asList(source.urls));
      this.dependencies.addAll(source.dependencies);
      this.privilegedExportedPackages.addAll(source.privilegedExportedPackages);
      this.privilegedArtifacts.addAll(source.privilegedArtifacts);
    }

    /**
     * Indicates which package are exported on the model.
     *
     * @param packages packages to export. Non null.
     * @return same builder instance.
     */
    public ClassLoaderModelBuilder exportingPackages(Set<String> packages) {
      checkArgument(packages != null, "packages cannot be null");
      this.packages.addAll(packages);
      return this;
    }

    /**
     * Indicates which resource are exported on the model.
     *
     * @param resources resources to export. Non null.
     * @return same builder instance.
     */
    public ClassLoaderModelBuilder exportingResources(Set<String> resources) {
      checkArgument(resources != null, "resources cannot be null");
      resources.stream().forEach(r -> this.resources.add(separatorsToUnix(r)));
      return this;
    }

    /**
     * Indicates which Java packages are loaded from the artifact itself, even if some other artifact in the region exports it.
     * <p>
     * The reason to use this is a that if a plugin internally uses a certain version of a lib, providing certain packages,
     * another plugin exporting some of those packages must not modify the inner implementation used by the first plugin.
     *
     * @param packages packages to not import. Non null.
     * @return same builder instance.
     */
    public ClassLoaderModelBuilder withLocalPackages(Set<String> packages) {
      checkArgument(packages != null, "packages cannot be null");
      this.localPackages.addAll(packages);
      return this;
    }

    /**
     * Indicates which resources are loaded from the artifact itself, even if some other artifact in the region exports it.
     * <p>
     * The reason to use this is a that if a plugin internally uses a certain version of a lib, providing certain packages,
     * another plugin exporting some of those packages must not modify the inner implementation used by the first plugin.
     *
     * @param resources resources to not import. Non null.
     * @return same builder instance.
     */
    public ClassLoaderModelBuilder withLocalResources(Set<String> resources) {
      checkArgument(resources != null, "resources cannot be null");
      resources.stream().forEach(r -> this.localResources.add(separatorsToUnix(r)));
      return this;
    }

    /**
     * Indicates which Java packages are exported as privileged API on the model.
     *
     * @param packages Java packages names to export. Non null.
     * @param artifactIds artifact IDs that have access to the privileged API. No null.
     * @return same builder instance.
     */
    public ClassLoaderModelBuilder exportingPrivilegedPackages(Set<String> packages, Set<String> artifactIds) {
      checkArgument(packages != null, "packages cannot be null");
      checkArgument(artifactIds != null, "artifactIds cannot be null");
      checkArgument(packages.isEmpty() == artifactIds.isEmpty(),
                    "Both packages and artifactIds must be empty or non empty simultaneously");

      this.privilegedExportedPackages.addAll(packages);
      this.privilegedArtifacts.addAll(artifactIds);

      return this;
    }

    /**
     * Indicates which dependencies are required for this model.
     *
     * @param dependencies dependencies on which the model depends on. Non null.
     * @return same builder instance.
     */
    public ClassLoaderModelBuilder dependingOn(Set<BundleDependency> dependencies) {
      checkArgument(dependencies != null, "dependencies cannot be null");
      this.dependencies.addAll(dependencies);
      return this;
    }

    /**
     * Adds an {@link URL} to the model
     *
     * @param url indicates which resource to add. Non null.
     * @return same builder instance.
     */
    public ClassLoaderModelBuilder containing(URL url) {
      checkArgument(url != null, "url cannot be null");
      urls.add(url);
      return this;
    }

    /**
     * Sets this model to include test dependencies on class loader URL resolution.
     *
     * @param includeTestDependencies {@code true} to include test dependencies.
     * @return same builder instance.
     */
    public ClassLoaderModelBuilder includeTestDependencies(boolean includeTestDependencies) {
      this.includeTestDependencies = includeTestDependencies;
      return this;
    }

    /**
     * Does the filtering of exported packages and resources from the local ones.
     */
    private void filterExportedFromLocalResources() {
      localPackages.removeAll(packages);
      localResources.removeAll(resources);
    }

    /**
     * Creates a {@link ClassLoaderModel} with the current configuration.
     * @return a non null {@link ClassLoaderModel}
     */
    public ClassLoaderModel build() {
      // first we remove from local packages and resources those that are exported.
      filterExportedFromLocalResources();

      return new ClassLoaderModel(urls.toArray(new URL[0]),
                                  unmodifiableSet(packages), unmodifiableSet(resources),
                                  unmodifiableSet(localPackages), unmodifiableSet(localResources),
                                  unmodifiableSet(dependencies),
                                  unmodifiableSet(privilegedExportedPackages), unmodifiableSet(privilegedArtifacts),
                                  includeTestDependencies);
    }
  }
}
