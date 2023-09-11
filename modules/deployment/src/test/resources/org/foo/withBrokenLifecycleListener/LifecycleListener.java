/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.withBrokenLifecycleListener;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

public class LifecycleListener implements ArtifactLifecycleListener {

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext disposalContext) {
    throw new RuntimeException("Error during onArtifactDisposal");
  }
}
