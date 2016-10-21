/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.artifact;

import org.mule.runtime.config.spring.MuleArtifactContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingService;

/**
 * An artifact context contains all the information related to an {@link org.mule.runtime.module.artifact.Artifact} that contains
 * configuration.
 * <p/>
 * This object holds the created {@link MuleContext} and the {@link MuleArtifactContext} which holds the information related to
 * the application configuration and resources.
 *
 * @since 4.0
 */
public class ArtifactContext {

  private MuleArtifactContext muleArtifactContext;

  /**
   * Creates an {@link ArtifactContext}.
   *
   * @param muleArtifactContext the artifact context.
   */
  public ArtifactContext(MuleArtifactContext muleArtifactContext) {
    this.muleArtifactContext = muleArtifactContext;
  }

  /**
   * @return the artifact {@link MuleContext}
   */
  public MuleContext getMuleContext() {
    return this.muleArtifactContext.getMuleContext();
  }

  /**
   *
   * @return
   */
  public ConnectivityTestingService getConnectivityTestingService() {
    return muleArtifactContext.getConnectivityTestingService();
  }

}
