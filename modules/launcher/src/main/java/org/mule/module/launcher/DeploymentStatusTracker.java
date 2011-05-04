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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of the deployment status of all applications that have been
 * discovered during the Mule instance startup.
 */
public class DeploymentStatusTracker implements DeploymentListener
{

    public static enum DeploymentState
    {
        // Indicate that the deployment was not started yet
        PENDING,
        // The deployment is in progress
        DEPLOYING,
        // The deployment was finished with a failure
        FAILED,
        // The deployment was successfully finished
        DEPLOYED
    }

    protected Map<String, DeploymentState> deploymentStates = new ConcurrentHashMap<String, DeploymentState>();

    public Map<String, DeploymentState> getDeploymentStates()
    {
        return Collections.unmodifiableMap(deploymentStates);
    }

    public void onNewDeploymentDetected(String appName)
    {
        deploymentStates.put(appName, DeploymentState.PENDING);
    }

    public void onDeploymentStart(String appName)
    {
        setApplicationState(appName, DeploymentState.DEPLOYING);
    }

    public void onDeploymentSuccess(String appName)
    {
        setApplicationState(appName, DeploymentState.DEPLOYED);
    }

    public void onDeploymentFailure(String appName, Throwable failureCause)
    {
        setApplicationState(appName, DeploymentState.FAILED);
    }

    protected void setApplicationState(String appName, DeploymentState deploymentState)
    {
        if (deploymentStates.containsKey(appName))
        {
            deploymentStates.put(appName, deploymentState);
        }
    }
}
