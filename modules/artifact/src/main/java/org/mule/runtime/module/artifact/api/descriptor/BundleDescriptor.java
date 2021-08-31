/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.descriptor;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import java.util.Optional;

/**
 * Describes a bundle by its Maven coordinates.
 */
public final class BundleDescriptor {

  private static final String STRINGARTIFACT_FILENAME_SEPARATOR = "-";
  private String groupId;
  private String artifactId;
  private String version;
  private String baseVersion;
  private String type = "jar";
  private Optional<String> classifier = empty();
  private volatile String artifactFileName;

  private Object metadata;

  private BundleDescriptor() {}

  public String getGroupId() {
    return this.groupId;
  }

  public String getArtifactId() {
    return this.artifactId;
  }

  public String getBaseVersion() {
    return baseVersion;
  }

  public String getVersion() {
    return this.version;
  }

  public String getType() {
    return type;
  }

  public Optional<String> getClassifier() {
    return classifier;
  }

  public boolean isPlugin() {
    return classifier.map(classifier -> classifier.equals("mule-plugin")).orElse(false);
  }

  /**
   * @return any metadata associated the bundle.
   * 
   * @since 4.5
   */
  public Object getMetadata() {
    return metadata;
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
    if (!version.equals(that.version)) {
      return false;
    }
    if (!classifier.equals(that.classifier)) {
      return false;
    }
    return type.equals(that.type);
  }

  @Override
  public int hashCode() {
    int result = groupId.hashCode();
    result = 31 * result + artifactId.hashCode();
    result = 31 * result + version.hashCode();
    result = 31 * result + classifier.hashCode();
    result = 31 * result + type.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "BundleDescriptor{" +
        "groupId='" + groupId + '\'' +
        ", artifactId='" + artifactId + '\'' +
        ", baseVersion='" + baseVersion + '\'' +
        ", version='" + version + '\'' +
        ", type='" + type + '\'' +
        ", classifier=" + classifier +
        '}';
  }

  /**
   * @return the file name that corresponds to the artifact described by this instance
   */
  public String getArtifactFileName() {
    if (artifactFileName == null) {
      synchronized (this) {
        if (artifactFileName == null) {
          String fileName = artifactId;

          if (getVersion() != null) {
            fileName = fileName + STRINGARTIFACT_FILENAME_SEPARATOR + getVersion();
          }

          if (classifier.isPresent()) {
            fileName = fileName + STRINGARTIFACT_FILENAME_SEPARATOR + classifier.get();
          }

          artifactFileName = fileName;
        }
      }
    }

    return artifactFileName;
  }

  /**
   * Builder for creating a {@code BundleDescriptor}
   */
  public static class Builder {

    private static final String ARTIFACT_ID = "artifact id";
    private static final String VERSION = "version";
    private static final String BASE_VERSION = "base version";
    private static final String GROUP_ID = "group id";
    private static final String TYPE = "type";
    private static final String CLASSIFIER = "classifier";
    private static final String REQUIRED_FIELD_NOT_FOUND_TEMPLATE = "bundle cannot be created with null or empty %s";

    private final BundleDescriptor bundleDependency = new BundleDescriptor();

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
      validateIsNotEmpty(version, VERSION);
      bundleDependency.version = version;
      return this;
    }

    /**
     * This is the base version of the bundle.
     *
     * @param baseVersion the base version of the bundle. Cannot be null or empty.
     * @return the builder
     */
    public BundleDescriptor.Builder setBaseVersion(String baseVersion) {
      validateIsNotEmpty(baseVersion, BASE_VERSION);
      bundleDependency.baseVersion = baseVersion;
      return this;
    }

    /**
     * Sets the extension type of the bundle.
     *
     * @param type the type id of the bundle. Cannot be null or empty.
     * @return the builder
     */
    public BundleDescriptor.Builder setType(String type) {
      validateIsNotEmpty(type, TYPE);
      bundleDependency.type = type;
      return this;
    }

    /**
     * Sets the classifier of the bundle.
     *
     * @param classifier classifier of the bundle. Can by null
     * @return the builder
     */
    public BundleDescriptor.Builder setClassifier(String classifier) {
      bundleDependency.classifier = ofNullable(classifier);
      return this;
    }

    /**
     * Sets the metadata associated the bundle.
     *
     * @param metadata metadata associated the bundle. Can by null
     * @return the builder
     */
    public BundleDescriptor.Builder setMetadata(Object metadata) {
      bundleDependency.metadata = metadata;
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
