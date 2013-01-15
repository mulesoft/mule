/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
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
     * @param appName the name of the application being deployed
     */
    void onDeploymentStart(String appName);

    /**
     * Notifies that a deploy for a given application has successfully finished.
     *
     * @param appName the name of the application being deployed
     */
    void onDeploymentSuccess(String appName);

    /**
     * Notifies that a deploy for a given application has finished with a failure.
     *
     * @param appName the name of the application being deployed
     * @param cause       the cause of the failure
     */
    void onDeploymentFailure(String appName, Throwable cause);

    /**
     * Notifies that an un-deployment for a given application has started.
     *
     * @param appName the name of the application being un-deployed
     */
    void onUndeploymentStart(String appName);

    /**
     * Notifies that an un-deployment for a given application has successfully finished.
     *
     * @param appName the name of the application being un-deployed
     */
    void onUndeploymentSuccess(String appName);

    /**
     * Notifies that an un-deployment for a given application has finished with a failure.
     *
     * @param appName the name of the application being un-deployed
     * @param cause       the cause of the failure
     */
    void onUndeploymentFailure(String appName, Throwable cause);

    /**
     * Notifies the creation of the {@link MuleContext} for a given app.
     *
     * @param appName name of the application that owns the mule context
     * @param context mule context for the application being deployed
     */
    void onMuleContextCreated(String appName, MuleContext context);

    /**
     * Notifies the initialization of the {@link MuleContext} for a given app.
     *
     * @param appName name of the application that owns the mule context
     * @param context mule context for the application being deployed
     */
    void onMuleContextInitialised(String appName, MuleContext context);

    /**
     * Notifies the configuration of the {@link MuleContext} for a given app.
     *
     * @param appName name of the application that owns the mule context
     * @param context mule context for the application being deployed
     */
    void onMuleContextConfigured(String appName, MuleContext context);
}
