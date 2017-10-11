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
 * Defines a listener for deployment events for all Mule artifacts.
 * Use {@link ArtifactDeploymentListenerAdapter} to make this listener work as a {@link DeploymentListener}
 */
public interface ArtifactDeploymentListener
{

    /**
     * Notifies that a deploy for a given artifact has started.
     *
     * @param type the artifact type
     * @param artifactName the name of the artifact being deployed
     */
    void onDeploymentStart(ArtifactType type, String artifactName);

    /**
     * Notifies that a deploy for a given artifact has successfully finished.
     *
     * @param type the artifact type
     * @param artifactName the name of the artifact being deployed
     */
    void onDeploymentSuccess(ArtifactType type, String artifactName);

    /**
     * Notifies that a deploy for a given artifact has finished with a failure.
     *
     * @param type the artifact type
     * @param artifactName the name of the artifact being deployed
     * @param cause       the cause of the failure
     */
    void onDeploymentFailure(ArtifactType type, String artifactName, Throwable cause);

    /**
     * Notifies that an un-deployment for a given artifact has started.
     *
     * @param type the artifact type
     * @param artifactName the name of the artifact being un-deployed
     * @param type the artifact type
     */
    void onUndeploymentStart(ArtifactType type, String artifactName);

    /**
     * Notifies that an un-deployment for a given artifact has successfully finished.
     *
     * @param type the artifact type
     * @param artifactName the name of the artifact being un-deployed
     */
    void onUndeploymentSuccess(ArtifactType type, String artifactName);

    /**
     * Notifies that an un-deployment for a given artifact has finished with a failure.
     *
     * @param type the artifact type
     * @param artifactName the name of the artifact being un-deployed
     * @param cause       the cause of the failure
     */
    void onUndeploymentFailure(ArtifactType type, String artifactName, Throwable cause);

    /**
     * Notifies the creation of the {@link MuleContext} for a given artifact.
     *
     * @param artifactName name of the artifact that owns the mule context
     * @param context mule context for the artifact being deployed
     */
    void onMuleContextCreated(String artifactName, MuleContext context);

    /**
     * Notifies the initialization of the {@link MuleContext} for a given artifact.
     *
     * @param artifactName name of the artifact that owns the mule context
     * @param context mule context for the artifact being deployed
     */
    void onMuleContextInitialised(String artifactName, MuleContext context);

    /**
     * Notifies the configuration of the {@link MuleContext} for a given artifact.
     *
     * @param artifactName name of the artifact that owns the mule context
     * @param context mule context for the artifact being deployed
     */
    void onMuleContextConfigured(String artifactName, MuleContext context);

}
