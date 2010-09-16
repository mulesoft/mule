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
