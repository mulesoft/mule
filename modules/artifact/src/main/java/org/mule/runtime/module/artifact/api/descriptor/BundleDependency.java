/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.descriptor;

import static org.mule.runtime.module.artifact.api.descriptor.BundleScope.COMPILE;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Describes a dependency on a bundle.
 *
 * @since 4.0
 */
public final class BundleDependency {

  /**
   * @return a fresh {@link Builder} for creating a {@link BundleDependency}
   * @since 4.9
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * @return a fresh {@link Builder} for creating a {@link BundleDependency}
   * @since 4.9
   */
  public static Builder builder(BundleDependency template) {
    return new Builder(template);
  }

  private BundleDescriptor descriptor;
  private BundleScope scope;
  private URI bundleUri;
  private List<BundleDependency> additionalDependencies;
  private List<BundleDependency> transitiveDependencies;

  private Set<String> packages = emptySet();
  private Set<String> resources = emptySet();

  private BundleDependency() {}

  public BundleScope getScope() {
    return scope != null ? scope : COMPILE;
  }

  public BundleDescriptor getDescriptor() {
    return descriptor;
  }

  public URI getBundleUri() {
    return bundleUri;
  }

  /**
   * @deprecated use {@link BundleDependency#getAdditionalDependenciesList()}
   */
  @Deprecated
  public Set<BundleDependency> getAdditionalDependencies() {
    return new LinkedHashSet<>(additionalDependencies);
  }

  /**
   * @since 4.2.2, 4.3.0
   */
  public List<BundleDependency> getAdditionalDependenciesList() {
    return additionalDependencies;
  }

  /**
   * @deprecated use {@link BundleDependency#getTransitiveDependenciesList()}
   */
  @Deprecated
  public Set<BundleDependency> getTransitiveDependencies() {
    return new LinkedHashSet<>(transitiveDependencies);
  }

  /**
   * @since 4.2.2, 4.3.0
   */
  public List<BundleDependency> getTransitiveDependenciesList() {
    return transitiveDependencies;
  }

  public Set<String> getPackages() {
    return packages;
  }

  public Set<String> getResources() {
    return resources;
  }

  @Override
  public String toString() {
    return "BundleDependency{" +
        "descriptor=" + descriptor +
        ", scope=" + scope +
        ", bundleUri=" + bundleUri +
        ", additionalDependencies=" + additionalDependencies +
        ", packages=" + packages +
        ", resources=" + resources +
        '}';
  }

  /**
   * Builder for creating a {@link BundleDependency}
   */
  public static class Builder {


    private static final String BUNDLE_DESCRIPTOR = "bundle descriptor";
    private static final String REQUIRED_FIELD_IS_NULL = "bundle cannot be created with null %s";

    private final BundleDependency bundleDependency = new BundleDependency();

    public Builder() {}

    public Builder(BundleDependency template) {
      bundleDependency.bundleUri = template.bundleUri;
      bundleDependency.descriptor = template.descriptor;
      bundleDependency.scope = template.scope;
      bundleDependency.additionalDependencies = template.additionalDependencies;
      bundleDependency.transitiveDependencies = template.transitiveDependencies;
      bundleDependency.packages = requireNonNull(template.packages, () -> getNullFieldMessage("packages"));
      bundleDependency.resources = requireNonNull(template.resources, () -> getNullFieldMessage("resources"));
    }

    /**
     * This is the descriptor of the bundle.
     *
     * @param descriptor the version of the bundle. Cannot be null or empty.
     * @return the builder
     */
    public Builder setDescriptor(BundleDescriptor descriptor) {
      requireNonNull(descriptor, () -> getNullFieldMessage(BUNDLE_DESCRIPTOR));
      this.bundleDependency.descriptor = descriptor;

      return this;
    }

    /**
     * Sets the scope of the bundle.
     *
     * @param scope scope of the bundle. Non null
     * @return the builder
     */
    public Builder setScope(BundleScope scope) {
      requireNonNull(scope, "scope cannot be null");
      bundleDependency.scope = scope;

      return this;
    }

    public Builder setBundleUri(URI bundleUri) {
      this.bundleDependency.bundleUri = bundleUri;
      return this;
    }

    /**
     * @deprecated use {@link Builder#setAdditionalDependencies(List)}
     */
    @Deprecated
    public Builder setAdditionalDependencies(Set<BundleDependency> additionalDependencies) {
      this.bundleDependency.additionalDependencies = new ArrayList<>(additionalDependencies);
      return this;
    }

    /**
     * @since 4.2.2, 4.3.0
     */
    public Builder setAdditionalDependencies(List<BundleDependency> additionalDependencies) {
      this.bundleDependency.additionalDependencies = additionalDependencies;
      return this;
    }

    /**
     * @deprecated use {@link Builder#setTransitiveDependencies(List)}
     */
    @Deprecated
    public Builder setTransitiveDependencies(Set<BundleDependency> transitiveDependencies) {
      this.bundleDependency.transitiveDependencies = new ArrayList<>(transitiveDependencies);
      return this;
    }

    /**
     * @since 4.2.2, 4.3.0
     */
    public Builder setTransitiveDependencies(List<BundleDependency> transitiveDependencies) {
      this.bundleDependency.transitiveDependencies = transitiveDependencies;
      return this;
    }

    public Builder setPackages(Set<String> packages) {
      this.bundleDependency.packages = requireNonNull(packages, () -> getNullFieldMessage("packages"));
      return this;
    }

    public Builder setResources(Set<String> resources) {
      this.bundleDependency.resources = requireNonNull(resources, () -> getNullFieldMessage("resources"));
      return this;
    }

    /**
     * @return a {@code BundleDescriptor} with the previous provided parameters to the builder.
     */
    public BundleDependency build() {
      requireNonNull(bundleDependency.descriptor, () -> getNullFieldMessage(BUNDLE_DESCRIPTOR));

      return this.bundleDependency;
    }

    private String getNullFieldMessage(String field) {
      return format(REQUIRED_FIELD_IS_NULL, field);
    }
  }
}
