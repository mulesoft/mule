/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.descriptor;

import static java.lang.Boolean.FALSE;
import static java.util.Arrays.asList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains all the information needed to create a {@link ClassLoader} for a Mule artifact.
 */
public class ClassLoaderModel {

  /**
   * Defines a {@link ClassLoaderModel} with empty configuration
   */
  public static final ClassLoaderModel NULL_CLASSLOADER_MODEL =
      new ClassLoaderModel(new URL[0], new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(),
                           false);

  private final URL[] urls;
  private final Set<String> exportedPackages;
  private final Set<String> exportedResources;
  private final Set<BundleDependency> dependencies;
  private final Set<String> privilegedExportedPackages;
  private final Set<String> privilegedArtifacts;
  private final boolean includeTestDependencies;

  private ClassLoaderModel(URL[] urls, Set<String> exportedPackages, Set<String> exportedResources,
                           Set<BundleDependency> dependencies, Set<String> privilegedExportedPackages,
                           Set<String> privilegedArtifacts, boolean includeTestDependencies) {
    this.urls = urls;
    this.exportedPackages = exportedPackages;
    this.exportedResources = exportedResources;
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
   * @return the resource packages to be exported on the {@link ClassLoader}. Non null
   */
  public Set<String> getExportedResources() {
    return exportedResources;
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

    private Set<String> packages = new HashSet<>();
    private Set<String> resources = new HashSet<>();
    private List<URL> urls = new ArrayList<>();
    private Set<BundleDependency> dependencies = new HashSet<>();
    private Set<String> privilegedExportedPackages = new HashSet<>();
    private Set<String> privilegedArtifacts = new HashSet<>();
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
      this.urls = new ArrayList<>(asList(source.urls));
      this.dependencies.addAll(source.dependencies);
      this.privilegedExportedPackages.addAll(source.privilegedExportedPackages);
      this.privilegedArtifacts.addAll(source.privilegedArtifacts);
    }

    /**
     * Indicates which package are exported on the model.
     *
     * @param packages packages to export. No null.
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
     * @param resources resources to export. No null.
     * @return same builder instance.
     */
    public ClassLoaderModelBuilder exportingResources(Set<String> resources) {
      checkArgument(resources != null, "resources cannot be null");
      this.resources.addAll(resources);
      return this;
    }

    /**
     * Indicates which Java packages are exported as privileged API on the model.
     *
     * @param packages Java packages names to export. No null.
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
     * Creates a {@link ClassLoaderModel} with the current configuration.
     * @return a non null {@link ClassLoaderModel}
     */
    public ClassLoaderModel build() {
      return new ClassLoaderModel(urls.toArray(new URL[0]), packages, resources, dependencies, privilegedExportedPackages,
                                  privilegedArtifacts, includeTestDependencies);
    }
  }
}
