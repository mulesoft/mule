/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.descriptor;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import java.net.URI;

/**
 * Describes a dependency on a bundle.
 *
 * @since 4.0
 */
public class BundleDependency {

  private BundleDescriptor descriptor;
  private BundleScope scope;
  private URI bundleUri;

  private BundleDependency() {}

  public BundleScope getScope() {
    return scope;
  }

  public BundleDescriptor getDescriptor() {
    return descriptor;
  }

  public URI getBundleUri() {
    return bundleUri;
  }

  @Override
  public String toString() {
    return "BundleDependency{" +
        "descriptor=" + descriptor +
        ", scope=" + scope +
        ", bundleUri=" + bundleUri +
        '}';
  }

  /**
   * Builder for creating a {@link BundleDependency}
   */
  public static class Builder {


    private static final String BUNDLE_DESCRIPTOR = "bundle descriptor";
    private static final String REQUIRED_FIELD_IS_NULL = "bundle cannot be created with null %s";

    private BundleDependency bundleDependency = new BundleDependency();

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
