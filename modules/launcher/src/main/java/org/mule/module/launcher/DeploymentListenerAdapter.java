/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;


import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.mule.config.bootstrap.ArtifactType.APP;
import static org.mule.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.util.Preconditions.checkArgument;

import org.mule.api.MuleContext;
import org.mule.config.bootstrap.ArtifactType;
import org.mule.util.Preconditions;

import java.util.Collection;


/**
 * Adapts an {@link ArtifactDeploymentListener} to work as a {@link DeploymentListener}.
 */
public class DeploymentListenerAdapter implements DeploymentListener
{

    private static final Collection<ArtifactType> DEPLOYMENT_LISTENER_ARTIFACT_SUPPORTED_TYPES = asList(DOMAIN, APP);

    public static final String UNSUPPORTED_ARTIFACT_TYPE_ERROR = format("DeploymentListener only supports %s artifact types.", DEPLOYMENT_LISTENER_ARTIFACT_SUPPORTED_TYPES);

    private final ArtifactDeploymentListener artifactDeploymentListener;
    private final ArtifactType artifactType;

    /**
     *
     * @param artifactDeploymentListener the artifactDeploymentListener to adapt.
     * @param artifactType valid values are {@link ArtifactType#APP} and {@link ArtifactType#DOMAIN}
     * @throws IllegalArgumentException if the artifact type is invalid.
     */
    public DeploymentListenerAdapter(ArtifactDeploymentListener artifactDeploymentListener, ArtifactType artifactType)
    {
        checkArgument(DEPLOYMENT_LISTENER_ARTIFACT_SUPPORTED_TYPES.contains(artifactType), UNSUPPORTED_ARTIFACT_TYPE_ERROR);
        this.artifactType = artifactType;
        this.artifactDeploymentListener = artifactDeploymentListener;
    }

    @Override
    public void onDeploymentStart(String artifactName)
    {
        artifactDeploymentListener.onDeploymentStart(artifactType, artifactName);
    }

    @Override
    public void onDeploymentSuccess(String artifactName)
    {
        artifactDeploymentListener.onDeploymentSuccess(artifactType, artifactName);
    }

    @Override
    public void onDeploymentFailure(String artifactName, Throwable cause)
    {
        artifactDeploymentListener.onDeploymentFailure(artifactType, artifactName, cause);
    }

    @Override
    public void onUndeploymentStart(String artifactName)
    {
        artifactDeploymentListener.onUndeploymentStart(artifactType, artifactName);
    }

    @Override
    public void onUndeploymentSuccess(String artifactName)
    {
        artifactDeploymentListener.onUndeploymentSuccess(artifactType, artifactName);
    }

    @Override
    public void onUndeploymentFailure(String artifactName, Throwable cause)
    {
        artifactDeploymentListener.onUndeploymentFailure(artifactType, artifactName, cause);
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

