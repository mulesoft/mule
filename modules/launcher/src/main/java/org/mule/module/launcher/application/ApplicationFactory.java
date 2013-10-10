/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import org.mule.MuleCoreExtension;
import org.mule.module.launcher.AppBloodhound;
import org.mule.module.launcher.DefaultAppBloodhound;
import org.mule.module.launcher.DeploymentService;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;

import java.io.IOException;
import java.util.Map;

/**
 * Responsible for creating application objects. E.g. handles the default/priviledged app,
 * wrapper objects, etc.
 */
public class ApplicationFactory
{
    protected DeploymentService deploymentService;
    protected Map<Class<? extends MuleCoreExtension>, MuleCoreExtension> coreExtensions;

    public ApplicationFactory(DeploymentService deploymentService, Map<Class<? extends MuleCoreExtension>, MuleCoreExtension> coreExtensions)
    {
        this.deploymentService = deploymentService;
        this.coreExtensions = coreExtensions;
    }

    public Application createApp(String appName) throws IOException
    {
        AppBloodhound bh = new DefaultAppBloodhound();
        final ApplicationDescriptor descriptor = bh.fetch(appName);
        if (descriptor.isPrivileged())
        {
            final PriviledgedMuleApplication delegate = new PriviledgedMuleApplication(descriptor);
            delegate.setDeploymentService(deploymentService);
            delegate.setCoreExtensions(coreExtensions);
            return new ApplicationWrapper(delegate);
        }
        else
        {
            final DefaultMuleApplication delegate = new DefaultMuleApplication(descriptor);
            delegate.setDeploymentService(deploymentService);
            return new ApplicationWrapper(delegate);
        }
    }
}
