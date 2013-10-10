/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
}
