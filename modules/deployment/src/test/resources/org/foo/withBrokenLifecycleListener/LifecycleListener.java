/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
