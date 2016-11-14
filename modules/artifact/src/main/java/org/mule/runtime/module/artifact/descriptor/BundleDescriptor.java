/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.descriptor;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * Describes a bundle by its Maven coordinates.
 */
public class BundleDescriptor {

  private String groupId;
  private String artifactId;
  private String version;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BundleDescriptor that = (BundleDescriptor) o;

    if (!groupId.equals(that.groupId)) {
      return false;
    }
    if (!artifactId.equals(that.artifactId)) {
      return false;
    }
    return version.equals(that.version);

  }

  @Override
  public int hashCode() {
    int result = groupId.hashCode();
    result = 31 * result + artifactId.hashCode();
    result = 31 * result + version.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "BundleDescriptor{" +
        "groupId='" + groupId + '\'' +
        ", artifactId='" + artifactId + '\'' +
        ", version='" + version + '\'' +
        '}';
  }

  /**
   * Builder for creating a {@code BundleDescriptor}
   */
  public static class Builder {

    private static final String ARTIFACT_ID = "artifact id";
    private static final String VERSION = "version";
    private static final String GROUP_ID = "group id";
    private static final String REQUIRED_FIELD_NOT_FOUND_TEMPLATE = "bundle cannot be created with null or empty %s";

    private BundleDescriptor bundleDependency = new BundleDescriptor();

    /**
     * @param groupId the group id of the bundle. Cannot be null or empty.
     * @return the builder
     */
    public BundleDescriptor.Builder setGroupId(String groupId) {
      validateIsNotEmpty(groupId, GROUP_ID);
      bundleDependency.groupId = groupId;
      return this;
    }

    /**
     * @param artifactId the artifactId id of the bundle. Cannot be null or empty.
     * @return the builder
     */
    public BundleDescriptor.Builder setArtifactId(String artifactId) {
      validateIsNotEmpty(artifactId, ARTIFACT_ID);
      bundleDependency.artifactId = artifactId;
      return this;
    }

    /**
     * This is the version of the bundle.
     *
     * @param version the version of the bundle. Cannot be null or empty.
     * @return the builder
     */
    public BundleDescriptor.Builder setVersion(String version) {
      validateIsNotEmpty(version, ARTIFACT_ID);
      bundleDependency.version = version;
      return this;
    }

    /**
     * @return a {@code BundleDescriptor} with the previous provided parameters to the builder.
     */
    public BundleDescriptor build() {
      validateIsNotEmpty(bundleDependency.groupId, GROUP_ID);
      validateIsNotEmpty(bundleDependency.artifactId, ARTIFACT_ID);
      validateIsNotEmpty(bundleDependency.version, VERSION);

      return this.bundleDependency;
    }

    private String getNullFieldMessage(String field) {
      return format(REQUIRED_FIELD_NOT_FOUND_TEMPLATE, field);
    }

    private void validateIsNotEmpty(String value, String fieldId) {
      checkState(!isEmpty(value), getNullFieldMessage(fieldId));
    }

    private static boolean isEmpty(String value) {
      return value == null || value.equals("");
    }
  }
}
