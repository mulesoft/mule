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
 */
public interface ArtifactDeploymentListener
{

    /**
     * Notifies that a deploy for a given artifact has started.
     *
     * @param artifactName the name of the artifact being deployed
     * @param type the artifact type
     */
    void onDeploymentStart(String artifactName, ArtifactType type);

    /**
     * Notifies that a deploy for a given artifact has successfully finished.
     *
     * @param artifactName the name of the artifact being deployed
     * @param type the artifact type
     */
    void onDeploymentSuccess(String artifactName, ArtifactType type);

    /**
     * Notifies that a deploy for a given artifact has finished with a failure.
     *
     * @param artifactName the name of the artifact being deployed
     * @param type the artifact type
     * @param cause       the cause of the failure
     */
    void onDeploymentFailure(String artifactName, ArtifactType type, Throwable cause);

    /**
     * Notifies that an un-deployment for a given artifact has started.
     *
     * @param artifactName the name of the artifact being un-deployed
     * @param type the artifact type
     */
    void onUndeploymentStart(String artifactName, ArtifactType type);

    /**
     * Notifies that an un-deployment for a given artifact has successfully finished.
     *
     * @param artifactName the name of the artifact being un-deployed
     * @param type the artifact type
     */
    void onUndeploymentSuccess(String artifactName, ArtifactType type);

    /**
     * Notifies that an un-deployment for a given artifact has finished with a failure.
     *
     * @param artifactName the name of the artifact being un-deployed
     * @param cause       the cause of the failure
     */
    void onUndeploymentFailure(String artifactName, ArtifactType type, Throwable cause);

    /**
     * Notifies the creation of the {@link MuleContext} for a given app.
     *
     * @param artifactName name of the artifact that owns the mule context
     * @param context mule context for the artifact being deployed
     */
    void onMuleContextCreated(String artifactName, MuleContext context);

    /**
     * Notifies the initialization of the {@link MuleContext} for a given app.
     *
     * @param artifactName name of the artifact that owns the mule context
     * @param context mule context for the artifact being deployed
     */
    void onMuleContextInitialised(String artifactName, MuleContext context);

    /**
     * Notifies the configuration of the {@link MuleContext} for a given app.
     *
     * @param artifactName name of the artifact that owns the mule context
     * @param context mule context for the artifact being deployed
     */
    void onMuleContextConfigured(String artifactName, MuleContext context);

}
