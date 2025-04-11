/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config.bootstrap;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleRuntimeException;

import java.util.Set;

/**
 * List the possible types of deployable artifacts
 *
 * @since 3.7.0
 *
 * @deprecated Since 4.9 use {@link org.mule.runtime.api.artifact.ArtifactType} instead.
 */
@Deprecated(since = "4.9")
public enum ArtifactType {

  APP(org.mule.runtime.api.artifact.ArtifactType.APP, "app"),

  DOMAIN(org.mule.runtime.api.artifact.ArtifactType.DOMAIN, "domain"),

  PLUGIN(org.mule.runtime.api.artifact.ArtifactType.PLUGIN, "plugin"),

  POLICY(org.mule.runtime.api.artifact.ArtifactType.POLICY, "policy"),

  SERVICE(org.mule.runtime.api.artifact.ArtifactType.SERVICE, "service"),

  SERVER_PLUGIN(org.mule.runtime.api.artifact.ArtifactType.SERVER_PLUGIN, "serverPlugin"),

  /**
   * @deprecated Use a {@link Set} of applicable {@link ArtifactType}s where possible.
   */
  @Deprecated
  ALL(null, "app/domain");

  public static final String APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY = "applyToArtifactType";
  private final org.mule.runtime.api.artifact.ArtifactType actualArtifactType;
  private final String artifactTypeAsString;

  ArtifactType(org.mule.runtime.api.artifact.ArtifactType artifactType, String artifactTypeAsString) {
    this.actualArtifactType = artifactType;
    this.artifactTypeAsString = artifactTypeAsString;
  }

  public org.mule.runtime.api.artifact.ArtifactType getArtifactType() {
    return actualArtifactType;
  }

  public String getAsString() {
    return this.artifactTypeAsString;
  }

  public static ArtifactType createFromString(String artifactTypeAsString) {
    for (ArtifactType artifactType : values()) {
      if (artifactType.artifactTypeAsString.equals(artifactTypeAsString)) {
        return artifactType;
      }
    }
    throw new MuleRuntimeException(createStaticMessage("No artifact type found for value: " + artifactTypeAsString));
  }
}
