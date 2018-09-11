/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.descriptor;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class ResourceArtifactDescriptor {

  private static final String WILDCARD = "*";

  private String groupId;
  private String artifactId;
  private String version;
  private String classifier;
  private String type;

  private ResourceArtifactDescriptor(String groupId, String artifactId, String version, String classifier, String type) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.classifier = classifier;
    this.type = type;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public String getClassifier() {
    return classifier;
  }

  public String getType() {
    return type;
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

  /**
   * Particular equals which considers {@code *} in the version as well as a full match. Stored objects will always feature a
   * proper version (since they represent actual artifacts) so only in the case of a request objects will that scenario be
   * possible, which is exactly what we want to consider a match.
   *
   * @return whether the descriptors are equal
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ResourceArtifactDescriptor that = (ResourceArtifactDescriptor) o;

    if (!groupId.equals(that.groupId)) {
      return false;
    }
    if (!artifactId.equals(that.artifactId)) {
      return false;
    }
    if (!version.equals(that.version) && !WILDCARD.equals(that.version)) {
      return false;
    }
    if (!classifier.equals(that.classifier)) {
      return false;
    }
    return type.equals(that.type);
  }

  public static class Builder {

    private static final String FAILURE_MESSAGE = "Resource artifact %s cannot be %s.";

    private String groupId;
    private String artifactId;
    private String version;
    private String classifier = "";
    private String type = "jar";

    public Builder groupId(String groupId) {
      this.groupId = requireNonEmpty(groupId, "group ID");
      return this;
    }

    public Builder artifactId(String artifactId) {
      this.artifactId = requireNonEmpty(artifactId, "ID");
      return this;
    }

    public Builder version(String version) {
      this.version = requireNonEmpty(version, "version");
      return this;
    }

    public Builder classifier(String classifier) {
      this.classifier = requireNonNull(classifier, format(FAILURE_MESSAGE, "classifier", "null"));
      return this;
    }

    public Builder type(String type) {
      this.type = requireNonEmpty(type, "type");
      return this;
    }

    public ResourceArtifactDescriptor build() {
      return new ResourceArtifactDescriptor(groupId, artifactId, version, classifier, type);
    }

    private String requireNonEmpty(String value, String component) {
      if (value == null || "".equals(value)) {
        throw new IllegalArgumentException(format(FAILURE_MESSAGE, component, "empty"));
      }
      return value;
    }

  }

}
