/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.dependency;

import org.mule.runtime.deployment.model.api.plugin.dependency.ArtifactDependency;

/**
 * Default implementation of ArtifactDependency
 * @since 4.0
 */
public class DefaultArtifactDependency implements ArtifactDependency {

  private final String groupId;
  private final String artifactId;
  private final String version;
  private final String type;
  private final String classifier;

  public DefaultArtifactDependency(String groupId, String artifactId, String version, String type, String classifier) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.type = type;
    this.classifier = classifier == null ? "" : classifier;
  }

  @Override
  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String getClassifier() {
    return classifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DefaultArtifactDependency that = (DefaultArtifactDependency) o;

    if (!groupId.equals(that.groupId)) {
      return false;
    }
    if (!artifactId.equals(that.artifactId)) {
      return false;
    }
    if (!version.equals(that.version)) {
      return false;
    }
    if (!type.equals(that.type)) {
      return false;
    }
    return classifier.equals(that.classifier);

  }

  @Override
  public int hashCode() {
    int result = groupId.hashCode();
    result = 31 * result + artifactId.hashCode();
    result = 31 * result + version.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + classifier.hashCode();
    return result;
  }
}
