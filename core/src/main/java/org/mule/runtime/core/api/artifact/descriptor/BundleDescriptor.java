/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.artifact.descriptor;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class BundleDescriptor {

  public static final String MULE_PLUGIN_CLASSIFIER = "mule-plugin";
  private static final String ARTIFACT_FILENAME_SEPARATOR = "-";

  protected String groupId;
  protected String artifactId;
  protected String version;
  protected String baseVersion;
  protected String type = "jar";
  protected Optional<String> classifier = empty();
  protected volatile String artifactFileName;
  protected Map<String, Object> metadata = emptyMap();

  protected BundleDescriptor() {}

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
    return classifier.map(MULE_PLUGIN_CLASSIFIER::equals).orElse(false);
  }

  /**
   * @return any metadata associated the bundle.
   */
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o instanceof BundleDescriptor) {
      BundleDescriptor that = (BundleDescriptor) o;
      return Objects.equals(groupId, that.groupId)
          && Objects.equals(artifactId, that.artifactId)
          && Objects.equals(version, that.version)
          && Objects.equals(classifier, that.classifier)
          && Objects.equals(type, that.type);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return hash(groupId, artifactId, version, classifier, type);
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
            fileName = fileName + ARTIFACT_FILENAME_SEPARATOR + getVersion();
          }

          if (classifier.isPresent()) {
            fileName = fileName + ARTIFACT_FILENAME_SEPARATOR + classifier.get();
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
     * @param metadata metadata associated the bundle. Cannot be null
     * @return the builder
     */
    public BundleDescriptor.Builder setMetadata(Map<String, Object> metadata) {
      bundleDependency.metadata = requireNonNull(metadata);
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
