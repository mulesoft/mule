/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.api.MuleContext;
import org.mule.config.bootstrap.ArtifactType;

/**
 * Default implementation of {@link ArtifactDeploymentListener}).
 * Method implementations are no-op.
 * Use it for implementing the ArtifactDeploymentListener interface
 * without having to override all methods.
 */
public abstract class AbstractArtifactDeploymentListener implements ArtifactDeploymentListener
{

    @Override
    public void onDeploymentStart(ArtifactType type, String artifactName)
    {
        //No-op default
    }

    @Override
    public void onDeploymentSuccess(ArtifactType type, String artifactName)
    {
        //No-op default
    }

    @Override
    public void onDeploymentFailure(ArtifactType type, String artifactName, Throwable cause)
    {
        //No-op default
    }

    @Override
    public void onUndeploymentStart(ArtifactType type, String artifactName)
    {
        //No-op default
    }

    @Override
    public void onUndeploymentSuccess(ArtifactType type, String artifactName)
    {
        //No-op default
    }

    @Override
    public void onUndeploymentFailure(ArtifactType type, String artifactName, Throwable cause)
    {
        //No-op default
    }

    @Override
    public void onMuleContextCreated(String artifactName, MuleContext context)
    {
        //No-op default
    }

    @Override
    public void onMuleContextInitialised(String artifactName, MuleContext context)
    {
        //No-op default
    }

    @Override
    public void onMuleContextConfigured(String artifactName, MuleContext context)
    {
        //No-op default
    }
}
