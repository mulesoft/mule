/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
