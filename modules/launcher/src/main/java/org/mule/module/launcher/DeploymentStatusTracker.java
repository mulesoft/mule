/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
