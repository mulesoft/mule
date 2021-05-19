/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.config;

import org.mule.runtime.api.meta.MuleVersion;

/**
 * Decoupled context used to evaluate {@link org.mule.runtime.api.config.Feature} flags.
 * 
 * @since 4.4.0
 */
public class FeatureContext {

  private String artifactName;
  private final MuleVersion artifactMinMuleVersion;

  public FeatureContext(MuleVersion artifactMinMuleVersion, String artifactName) {
    this.artifactMinMuleVersion = artifactMinMuleVersion;
    this.artifactName = artifactName;
  }

  public MuleVersion getArtifactMinMuleVersion() {
    return artifactMinMuleVersion;
  }

  public String getArtifactName() {
    return artifactName;
  }
}
