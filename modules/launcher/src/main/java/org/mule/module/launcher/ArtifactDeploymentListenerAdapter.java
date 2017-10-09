/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;


import static org.mule.config.bootstrap.ArtifactType.APP;
import static org.mule.config.bootstrap.ArtifactType.DOMAIN;
import org.mule.api.MuleContext;
import org.mule.config.bootstrap.ArtifactType;

/**
 * Adapts a {@link DeploymentListener} to an {@link ArtifactDeploymentListener}.
 */
public class ArtifactDeploymentListenerAdapter
{

    private DeploymentListener adaptedDomainDeploymentListener;
    private DeploymentListener adaptedApplicationDeploymentListener;
    private ArtifactDeploymentListener artifactDeploymentListener;

    public ArtifactDeploymentListenerAdapter(ArtifactDeploymentListener artifactDeploymentListener)
    {
        this.artifactDeploymentListener = artifactDeploymentListener;
    }

    public void adapt()
    {
        adaptedDomainDeploymentListener = new AdaptedDeploymentListener(artifactDeploymentListener, DOMAIN);
        adaptedApplicationDeploymentListener = new AdaptedDeploymentListener(artifactDeploymentListener, APP);
    }

    public DeploymentListener getAdaptedDomainDeploymentListener()
    {
        return adaptedDomainDeploymentListener;
    }

    public DeploymentListener getAdaptedApplicationDeploymentListener()
    {
        return adaptedApplicationDeploymentListener;
    }

    private class AdaptedDeploymentListener implements DeploymentListener
    {

        private final ArtifactDeploymentListener artifactDeploymentListener;
        private final ArtifactType artifactType;

        public AdaptedDeploymentListener(ArtifactDeploymentListener artifactDeploymentListener, ArtifactType artifactType)
        {
            this.artifactType = artifactType;
            this.artifactDeploymentListener = artifactDeploymentListener;
        }

        @Override
        public void onDeploymentStart(String artifactName)
        {
            artifactDeploymentListener.onDeploymentStart(artifactName, artifactType);
        }

        @Override
        public void onDeploymentSuccess(String artifactName)
        {
            artifactDeploymentListener.onDeploymentSuccess(artifactName, artifactType);
        }

        @Override
        public void onDeploymentFailure(String artifactName, Throwable cause)
        {
            artifactDeploymentListener.onDeploymentFailure(artifactName, artifactType, cause);
        }

        @Override
        public void onUndeploymentStart(String artifactName)
        {
            artifactDeploymentListener.onUndeploymentStart(artifactName, artifactType);
        }

        @Override
        public void onUndeploymentSuccess(String artifactName)
        {
            artifactDeploymentListener.onUndeploymentSuccess(artifactName, artifactType);
        }

        @Override
        public void onUndeploymentFailure(String artifactName, Throwable cause)
        {
            artifactDeploymentListener.onUndeploymentFailure(artifactName, artifactType, cause);
        }

        @Override
        public void onMuleContextCreated(String artifactName, MuleContext context)
        {
            artifactDeploymentListener.onMuleContextCreated(artifactName, context);
        }

        @Override
        public void onMuleContextInitialised(String artifactName, MuleContext context)
        {
            artifactDeploymentListener.onMuleContextInitialised(artifactName, context);
        }

        @Override
        public void onMuleContextConfigured(String artifactName, MuleContext context)
        {
            artifactDeploymentListener.onMuleContextConfigured(artifactName, context);
        }
    }
}
