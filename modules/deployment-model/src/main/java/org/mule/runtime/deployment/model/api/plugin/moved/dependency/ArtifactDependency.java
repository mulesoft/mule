/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.plugin.moved.dependency;

import org.mule.runtime.deployment.model.internal.plugin.moved.dependency.DefaultArtifactDependency;

/**
 * Represents an artifact dependency
 *
 * @since 4.0
 */
public interface ArtifactDependency {

  /**
   * @return The project group that produced the dependency, e.g. "org.apache.maven"
   */
  String getGroupId();

  /**
   * @return The unique id for an artifact produced by the project group, e.g. "maven-artifact".
   */
  String getArtifactId();

  /**
   * @return The version of the dependency, e.g. "4.1.3". It could also be specified as a range of versions.
   */
  String getVersion();

  /**
   * @return The type of dependency.
   */
  String getType();

  /**
   * @return The classifier of the dependency, e.g. "mule-plugin"
   */
  String getClassifier();

  /**
   * @return The scope of the dependency.
   * TODO MULE-10785 remove this class
   */
  Scope getScope();

  /**
   * TODO MULE-10785 remove this one
   */
  static ArtifactDependency create(String groupId, String artifactId, String version,
                                   String type, String classifier,
                                   Scope scope) {
    return new DefaultArtifactDependency(groupId, artifactId, version, type, classifier, scope);
  }
}
