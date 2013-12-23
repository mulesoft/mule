/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.api.MuleContext;

/**
 * Defines a listener for deployment events for Mule applications.
 */
public interface DeploymentListener
{

    /**
     * Notifies that a deploy for a given application has started.
     *
     * @param artifactName the name of the application being deployed
     */
    void onDeploymentStart(String artifactName);

    /**
     * Notifies that a deploy for a given application has successfully finished.
     *
     * @param artifactName the name of the application being deployed
     */
    void onDeploymentSuccess(String artifactName);

    /**
     * Notifies that a deploy for a given application has finished with a failure.
     *
     * @param artifactName the name of the application being deployed
     * @param cause       the cause of the failure
     */
    void onDeploymentFailure(String artifactName, Throwable cause);

    /**
     * Notifies that an un-deployment for a given application has started.
     *
     * @param artifactName the name of the application being un-deployed
     */
    void onUndeploymentStart(String artifactName);

    /**
     * Notifies that an un-deployment for a given application has successfully finished.
     *
     * @param artifactName the name of the application being un-deployed
     */
    void onUndeploymentSuccess(String artifactName);

    /**
     * Notifies that an un-deployment for a given application has finished with a failure.
     *
     * @param artifactName the name of the application being un-deployed
     * @param cause       the cause of the failure
     */
    void onUndeploymentFailure(String artifactName, Throwable cause);

    /**
     * Notifies the creation of the {@link MuleContext} for a given app.
     *
     * @param artifactName name of the application that owns the mule context
     * @param context mule context for the application being deployed
     */
    void onMuleContextCreated(String artifactName, MuleContext context);

    /**
     * Notifies the initialization of the {@link MuleContext} for a given app.
     *
     * @param artifactName name of the application that owns the mule context
     * @param context mule context for the application being deployed
     */
    void onMuleContextInitialised(String artifactName, MuleContext context);

    /**
     * Notifies the configuration of the {@link MuleContext} for a given app.
     *
     * @param artifactName name of the application that owns the mule context
     * @param context mule context for the application being deployed
     */
    void onMuleContextConfigured(String artifactName, MuleContext context);
}
