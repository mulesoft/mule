/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.config;

import org.mule.runtime.api.meta.MuleVersion;

// TODO: See if this can be done more "predicate friendly" (easier chain of multiple conditions)
public class FeatureContext {

  private MuleVersion artifactMinMuleVersion;

  public FeatureContext(MuleVersion artifactMinMuleVersion) {
    this.artifactMinMuleVersion = artifactMinMuleVersion;
  }

  public MuleVersion getArtifactMinMuleVersion() {
    return artifactMinMuleVersion;
  }
}
