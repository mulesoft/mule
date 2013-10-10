/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher;

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
    public void onDeploymentStart(String appName)
    {
        //No-op default
    }

    @Override
    public void onDeploymentSuccess(String appName)
    {
        //No-op default
    }

    @Override
    public void onDeploymentFailure(String appName, Throwable cause)
    {
        //No-op default
    }

    @Override
    public void onUndeploymentStart(String appName)
    {
        //No-op default
    }

    @Override
    public void onUndeploymentSuccess(String appName)
    {
        //No-op default
    }

    @Override
    public void onUndeploymentFailure(String appName, Throwable cause)
    {
        //No-op default
    }

}