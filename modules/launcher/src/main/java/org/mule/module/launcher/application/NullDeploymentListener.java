/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.application;

import org.mule.api.MuleContext;
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

    @Override
    public void onMuleContextCreated(String appName, MuleContext context)
    {
    }

    @Override
    public void onMuleContextInitialised(String appName, MuleContext context)
    {
    }

    @Override
    public void onMuleContextConfigured(String appName, MuleContext context)
    {
    }
}
