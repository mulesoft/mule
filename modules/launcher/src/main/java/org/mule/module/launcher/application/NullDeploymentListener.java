/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher.application;

import org.mule.module.launcher.DeploymentListener;

/**
 * Defines a {@link org.mule.module.launcher.DeploymentListener} that does nothing
 */
public class NullDeploymentListener implements DeploymentListener
{

    @Override
    public void onDeploymentStart(String appName)
    {
    }

    @Override
    public void onDeploymentSuccess(String appName)
    {
    }

    @Override
    public void onDeploymentFailure(String appName, Throwable cause)
    {
    }

    @Override
    public void onUndeploymentStart(String appName)
    {
    }

    @Override
    public void onUndeploymentSuccess(String appName)
    {
    }

    @Override
    public void onUndeploymentFailure(String appName, Throwable cause)
    {
    }
}
