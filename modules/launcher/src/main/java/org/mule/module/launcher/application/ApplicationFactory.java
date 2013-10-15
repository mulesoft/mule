/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import org.mule.module.launcher.AppBloodhound;
import org.mule.module.launcher.DefaultAppBloodhound;
import org.mule.module.launcher.DeploymentService;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;

import java.io.IOException;

/**
 * Responsible for creating application objects. E.g. handles the default/priviledged app,
 * wrapper objects, etc.
 */
public class ApplicationFactory
{
    protected DeploymentService deploymentService;

    public ApplicationFactory(DeploymentService deploymentService)
    {
        this.deploymentService = deploymentService;
    }

    public Application createApp(String appName) throws IOException
    {
        AppBloodhound bh = new DefaultAppBloodhound();
        final ApplicationDescriptor descriptor = bh.fetch(appName);
        if (descriptor.isPriviledged())
        {
            final PriviledgedMuleApplication delegate = new PriviledgedMuleApplication(appName);
            delegate.setDeploymentService(deploymentService);
            return new ApplicationWrapper(delegate);
        }
        else
        {
            return new ApplicationWrapper(new DefaultMuleApplication(appName));
        }
    }
}
