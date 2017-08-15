/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config.bootstrap;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.config.i18n.CoreMessages;

/**
 * List the possible types of deployable artifacts
 *
 * @since 3.7.0
 */
public enum ArtifactType {
  APP("app"), DOMAIN("domain"), PLUGIN("plugin"), POLICY("policy"), SERVICE("service"), SERVER_PLUGIN("serverPlugin"), ALL(
      "app/domain");

  public static final String APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY = "applyToArtifactType";
  private final String artifactTypeAsString;

  ArtifactType(String artifactTypeAsString) {
    this.artifactTypeAsString = artifactTypeAsString;
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
    throw new MuleRuntimeException(CoreMessages.createStaticMessage("No artifact type found for value: " + artifactTypeAsString));
  }
}
