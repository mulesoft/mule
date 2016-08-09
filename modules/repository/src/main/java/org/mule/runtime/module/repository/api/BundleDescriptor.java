/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.repository.api;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import com.google.common.base.Preconditions;

/**
 * Descriptor to identify a bundle
 *
 * @since 4.0
 */
public class BundleDescriptor {

  private String groupId;
  private String artifactId;
  private String version;
  private String type;

  private BundleDescriptor() {}

  public String getGroupId() {
    return this.groupId;
  }

  public String getArtifactId() {
    return this.artifactId;
  }

  public String getVersion() {
    return this.version;
  }

  public String getType() {
    return type;
  }

  public String toString() {
    return format("BundleDescriptor { %s:%s:%s:$s }", groupId, artifactId, version, type);
  }

  /**
   * Builder for creating a {@code BundleDescriptor}
   */
  public static class Builder {

    private static final String ARTIFACT_ID = "artifact id";
    private static final String VERSION = "version";
    private static final String TYPE = "type";
    private static final String GROUP_ID = "group id";
    private static final String REQUIRED_FIELD_NOT_FOUND_TEMPLATE = "bundle cannot be created with null or empty %s";

    private BundleDescriptor bundleDescriptor = new BundleDescriptor();

    /**
     * @param groupId the group id of the bundle. Cannot be null or empty.
     * @return the builder
     */
    public Builder setGroupId(String groupId) {
      validateIsNotEmpty(groupId, GROUP_ID);
      bundleDescriptor.groupId = groupId;
      return this;
    }

    /**
     * @param artifactId the artifactId id of the bundle. Cannot be null or empty.
     * @return the builder
     */
    public Builder setArtifactId(String artifactId) {
      validateIsNotEmpty(artifactId, ARTIFACT_ID);
      bundleDescriptor.artifactId = artifactId;
      return this;
    }

    /**
     * This is the version of the bundle.
     *
     * @param version the version of the bundle. Cannot be null or empty.
     * @return the builder
     */
    public Builder setVersion(String version) {
      validateIsNotEmpty(version, ARTIFACT_ID);
      bundleDescriptor.version = version;
      return this;
    }

    /**
     * Sets the extension type of the bundle.
     *
     * @param type the type id of the bundle. Cannot be null or empty.
     * @return the builder
     */
    public Builder setType(String type) {
      validateIsNotEmpty(type, TYPE);
      bundleDescriptor.type = type;
      return this;
    }

    /**
     * @return a {@code BundleDescriptor} with the previous provided parameters to the builder.
     */
    public BundleDescriptor build() {
      validateIsNotEmpty(bundleDescriptor.groupId, GROUP_ID);
      validateIsNotEmpty(bundleDescriptor.artifactId, ARTIFACT_ID);
      validateIsNotEmpty(bundleDescriptor.version, VERSION);
      validateIsNotEmpty(bundleDescriptor.type, TYPE);
      return this.bundleDescriptor;
    }

    private String getNullFieldMessage(String field) {
      return format(REQUIRED_FIELD_NOT_FOUND_TEMPLATE, field);
    }

    private void validateIsNotEmpty(String value, String fieldId) {
      checkState(!isEmpty(value), getNullFieldMessage(fieldId));
    }

    private boolean isEmpty(String value) {
      return value == null || value.equals("");
    }

  }


}
