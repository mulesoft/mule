/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.artifact.descriptor.api;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.artifact.ArtifactCoordinates;

import java.util.Map;
import java.util.Optional;

/**
 * Describes a bundle by its Maven coordinates.
 *
 * @since 4.9
 */
@NoImplement
public interface BundleDescriptor extends ArtifactCoordinates {

  String getBaseVersion();

  String getType();

  Optional<String> getClassifier();

  boolean isPlugin();

  /**
   * @return any metadata associated the bundle.
   */
  Map<String, Object> getMetadata();

  /**
   * @return the file name that corresponds to the artifact described by this instance
   */
  String getArtifactFileName();

  // /**
  // * Builder for creating a {@code BundleDescriptor}
  // */
  // interface Builder {
  //
  // /**
  // * @param groupId the group id of the bundle. Cannot be null or empty.
  // * @return the builder
  // */
  // BundleDescriptor.Builder setGroupId(String groupId);
  //
  // /**
  // * @param artifactId the artifactId id of the bundle. Cannot be null or empty.
  // * @return the builder
  // */
  // BundleDescriptor.Builder setArtifactId(String artifactId);
  //
  // /**
  // * This is the version of the bundle.
  // *
  // * @param version the version of the bundle. Cannot be null or empty.
  // * @return the builder
  // */
  // BundleDescriptor.Builder setVersion(String version);
  //
  // /**
  // * This is the base version of the bundle.
  // *
  // * @param baseVersion the base version of the bundle. Cannot be null or empty.
  // * @return the builder
  // */
  // BundleDescriptor.Builder setBaseVersion(String baseVersion);
  //
  // /**
  // * Sets the extension type of the bundle.
  // *
  // * @param type the type id of the bundle. Cannot be null or empty.
  // * @return the builder
  // */
  // BundleDescriptor.Builder setType(String type);
  //
  // /**
  // * Sets the classifier of the bundle.
  // *
  // * @param classifier classifier of the bundle. Can by null
  // * @return the builder
  // */
  // BundleDescriptor.Builder setClassifier(String classifier);
  //
  // /**
  // * Sets the metadata associated the bundle.
  // *
  // * @param metadata metadata associated the bundle. Cannot be null
  // * @return the builder
  // */
  // BundleDescriptor.Builder setMetadata(Map<String, Object> metadata);
  //
  // /**
  // * @return a {@code BundleDescriptor} with the previous provided parameters to the builder.
  // */
  // BundleDescriptor build();
  //
  // }

}
