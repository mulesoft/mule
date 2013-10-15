/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

/**
 * Defines a listener for deployment events.
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
}
