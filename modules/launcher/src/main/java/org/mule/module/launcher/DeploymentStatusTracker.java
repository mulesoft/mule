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
 * Keeps track of the deployment status of all applications in the Mule instance.
 */
public class DeploymentStatusTracker extends AbstractDeploymentListener
{

    public static enum DeploymentState
    {
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

    public void onDeploymentStart(String appName)
    {
        deploymentStates.put(appName, DeploymentState.DEPLOYING);
    }

    public void onDeploymentSuccess(String appName)
    {
        deploymentStates.put(appName, DeploymentState.DEPLOYED);
    }

    public void onDeploymentFailure(String appName, Throwable failureCause)
    {
        deploymentStates.put(appName, DeploymentState.FAILED);
    }

}
