/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.listener;

import org.mule.api.MuleContext;
import org.mule.module.launcher.DeploymentListener;

/**
 * Test deployment listener to obtain the muleContext
 * and verify its contents.
 */
public class TestDeploymentListener implements DeploymentListener
{

    private MuleContext muleContext;
    
    private String artifactName;
    
    @Override
    public void onDeploymentStart(String artifactName)
    {
        
    }

    @Override
    public void onDeploymentSuccess(String artifactName)
    {
    }

    @Override
    public void onDeploymentFailure(String artifactName, Throwable cause)
    {
    }

    @Override
    public void onUndeploymentStart(String artifactName)
    {
    }

    @Override
    public void onUndeploymentSuccess(String artifactName)
    {
    }

    @Override
    public void onUndeploymentFailure(String artifactName, Throwable cause)
    {
    }

    @Override
    public void onMuleContextCreated(String artifactName, MuleContext context)
    {
    }

    @Override
    public void onMuleContextInitialised(String artifactName, MuleContext context)
    {
        this.muleContext = context;
        this.artifactName = artifactName;
    }

    @Override
    public void onMuleContextConfigured(String artifactName, MuleContext context)
    {
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public String getArtifactName()
    {
        return artifactName;
    }
}
