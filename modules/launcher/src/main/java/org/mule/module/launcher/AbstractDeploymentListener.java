/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.api.MuleContext;

/**
 * Convenience implementation of DeploymentListener.  Default 
 * method implementations are no-ops.  Sub-classes can implement 
 * the DeploymentListener interface without having to override 
 * all methods.
 * 
 * This was implemented so that DeploymentStatusTracker would not need 
 * to provide default implementations of undeployment events.
 * 
 */
public class AbstractDeploymentListener implements DeploymentListener
{

    @Override
    public void onDeploymentStart(String artifactName)
    {
        //No-op default
    }

    @Override
    public void onDeploymentSuccess(String artifactName)
    {
        //No-op default
    }

    @Override
    public void onDeploymentFailure(String artifactName, Throwable cause)
    {
        //No-op default
    }

    @Override
    public void onUndeploymentStart(String artifactName)
    {
        //No-op default
    }

    @Override
    public void onUndeploymentSuccess(String artifactName)
    {
        //No-op default
    }

    @Override
    public void onUndeploymentFailure(String artifactName, Throwable cause)
    {
        //No-op default
    }

    @Override
    public void onMuleContextCreated(String artifactName, MuleContext context)
    {
        //No-op default
    }

    @Override
    public void onMuleContextInitialised(String artifactName, MuleContext context)
    {
        //No-op default
    }

    @Override
    public void onMuleContextConfigured(String artifactName, MuleContext context)
    {
        //No-op default
    }

}
