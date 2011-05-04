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

import org.mule.module.launcher.application.Application;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of the status of all applications and zombies that have been
 * discovered during the Mule instance startup.
 */
public class ApplicationStatusTracker implements DeploymentListener
{

    /**
     * Defines the different states that an application can be during the
     * deployment process.
     */
    public static enum ApplicationDeploymentState
    {
        PENDING,
        FAILED,
        DEPLOYING,
        RUNNING
    }

    private Map<String, ApplicationDeploymentState> applicationStates = new ConcurrentHashMap<String, ApplicationDeploymentState>();

    /**
     * Adds an application into the deployment status tracking.
     *
     * @param name the application name
     */
    public void addApplication(String name)
    {
        applicationStates.put(name, ApplicationDeploymentState.PENDING);
    }

    /**
     * Adds a zombie application into the deployment status tracking.
     *
     * @param name the zombie application name
     */
    public void addZombie(String name)
    {
        applicationStates.put(name, ApplicationDeploymentState.FAILED);
    }

    public Map<String, ApplicationDeploymentState> getApplicationStates()
    {
        return Collections.unmodifiableMap(applicationStates);
    }

    public void onDeploymentStart(Application application)
    {
        setApplicationState(application, ApplicationDeploymentState.DEPLOYING);
    }

    public void onDeploymentSuccess(Application application)
    {
        setApplicationState(application, ApplicationDeploymentState.RUNNING);
    }

    public void onDeploymentFailure(Application application, Throwable failureCause)
    {
        setApplicationState(application, ApplicationDeploymentState.FAILED);
    }

    private void setApplicationState(Application application, ApplicationDeploymentState deploymentState)
    {
        if (applicationStates.containsKey(application.getAppName()))
        {
            applicationStates.put(application.getAppName(), deploymentState);
        }
    }
}
