/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.descriptor;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import java.net.URL;

/**
 * Describes a dependency on a bundle.
 *
 * @since 4.0
 */
public class BundleDependency {

  private BundleDescriptor descriptor;
  private BundleScope scope;
  private URL bundleUrl;

  private BundleDependency() {}

  public BundleScope getScope() {
    return scope;
  }

  public BundleDescriptor getDescriptor() {
    return descriptor;
  }

  public URL getBundleUrl() {
    return bundleUrl;
  }

  @Override
  public String toString() {
    return "BundleDependency{" +
        "descriptor=" + descriptor +
        ", scope=" + scope +
        ", bundleUrl=" + bundleUrl +
        '}';
  }

  /**
   * Builder for creating a {@link BundleDependency}
   */
  public static class Builder {


    private static final String BUNDLE_DESCRIPTOR = "bundle descriptor";
    private static final String REQUIRED_FIELD_NOT_FOUND_TEMPLATE = "bundle cannot be created with null or empty %s";
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

    /**
     * Sets the descriptor of the bundle.
     *
     * @param descriptor describes the bundle which is dependent on. Cannot be null
     * @return the builder
     */
    public Builder sedBundleDescriptor(BundleDescriptor descriptor) {
      validateIsNotNull(descriptor, BUNDLE_DESCRIPTOR);
      this.bundleDependency.descriptor = descriptor;

      return this;
    }

    public Builder setBundleUrl(URL bundleUrl) {
      validateIsNotNull(bundleUrl, "Bundle URL cannot be null");
      this.bundleDependency.bundleUrl = bundleUrl;
      return this;
    }

    /**
     * @return a {@code BundleDescriptor} with the previous provided parameters to the builder.
     */
    public BundleDependency build() {
      validateIsNotNull(bundleDependency.descriptor, BUNDLE_DESCRIPTOR);

      return this.bundleDependency;
    }

    private String getEmptyFieldMessage(String field) {
      return format(REQUIRED_FIELD_NOT_FOUND_TEMPLATE, field);
    }

    private String getNullFieldMessage(String field) {
      return format(REQUIRED_FIELD_IS_NULL, field);
    }

    private void validateIsNotEmpty(String value, String fieldId) {
      checkState(!isEmpty(value), getEmptyFieldMessage(fieldId));
    }

    private static boolean isEmpty(String value) {
      return value == null || value.equals("");
    }

    private void validateIsNotNull(Object value, String fieldId) {
      checkState(value != null, getNullFieldMessage(fieldId));
    }
  }
}
