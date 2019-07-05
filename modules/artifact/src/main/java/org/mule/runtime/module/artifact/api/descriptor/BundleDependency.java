/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.descriptor;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.mule.runtime.module.artifact.api.descriptor.BundleScope.COMPILE;

import java.net.URI;
import java.util.Set;

/**
 * Describes a dependency on a bundle.
 *
 * @since 4.0
 */
public final class BundleDependency {

  private BundleDescriptor descriptor;
  private BundleScope scope;
  private URI bundleUri;
  private Set<BundleDependency> additionalDependencies;
  private Set<BundleDependency> transitiveDependencies;

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

  public Set<BundleDependency> getAdditionalDependencies() {
    return additionalDependencies;
  }

  public Set<BundleDependency> getTransitiveDependencies() {
    return transitiveDependencies;
  }

  @Override
  public String toString() {
    return "BundleDependency{" +
        "descriptor=" + descriptor +
        ", scope=" + scope +
        ", bundleUri=" + bundleUri +
        ", additionalDependencies=" + additionalDependencies +
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
    }

    /**
     * This is the descriptor of the bundle.
     *
     * @param descriptor the version of the bundle. Cannot be null or empty.
     * @return the builder
     */
    public Builder setDescriptor(BundleDescriptor descriptor) {
      validateIsNotNull(descriptor, BUNDLE_DESCRIPTOR);
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
      checkState(scope != null, "scope cannot be null");
      bundleDependency.scope = scope;

      return this;
    }

    public Builder setBundleUri(URI bundleUri) {
      this.bundleDependency.bundleUri = bundleUri;
      return this;
    }

    public Builder setAdditionalDependencies(Set<BundleDependency> additionalDependencies) {
      this.bundleDependency.additionalDependencies = additionalDependencies;
      return this;
    }

    public Builder setTransitiveDependencies(Set<BundleDependency> transitiveDependencies) {
      this.bundleDependency.transitiveDependencies = transitiveDependencies;
      return this;
    }

    /**
     * @return a {@code BundleDescriptor} with the previous provided parameters to the builder.
     */
    public BundleDependency build() {
      validateIsNotNull(bundleDependency.descriptor, BUNDLE_DESCRIPTOR);

      return this.bundleDependency;
    }

    private String getNullFieldMessage(String field) {
      return format(REQUIRED_FIELD_IS_NULL, field);
    }

    private void validateIsNotNull(Object value, String fieldId) {
      checkState(value != null, getNullFieldMessage(fieldId));
    }
  }
}
