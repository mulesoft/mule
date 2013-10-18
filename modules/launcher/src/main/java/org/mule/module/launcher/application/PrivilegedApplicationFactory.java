/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import org.mule.MuleCoreExtension;
import org.mule.module.launcher.DeploymentService;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;

import java.io.IOException;
import java.util.Map;

/**
 * Creates privileged and default mule applications
 */
public class PrivilegedApplicationFactory extends DefaultApplicationFactory
{

    private final DeploymentService deploymentService;
    private final Map<Class<? extends MuleCoreExtension>, MuleCoreExtension> coreExtensions;

    public PrivilegedApplicationFactory(DeploymentService deploymentService, Map<Class<? extends MuleCoreExtension>, MuleCoreExtension> coreExtensions)
    {
        this.deploymentService = deploymentService;
        this.coreExtensions = coreExtensions;
    }

    @Override
    protected Application createAppFrom(ApplicationDescriptor descriptor) throws IOException
    {
        if (descriptor.isPrivileged())
        {
            return createPriviledgedApp(descriptor);
        }
        else
        {
            return super.createAppFrom(descriptor);
        }
    }

    protected Application createPriviledgedApp(ApplicationDescriptor descriptor) throws IOException
    {
        final PriviledgedMuleApplication delegate = new PriviledgedMuleApplication(descriptor);

        if (deploymentListener != null)
        {
            delegate.setDeploymentListener(deploymentListener);
        }
        delegate.setDeploymentService(deploymentService);
        delegate.setCoreExtensions(coreExtensions);

        return new ApplicationWrapper(delegate);
    }
}
