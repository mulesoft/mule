/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import org.mule.api.registry.RegistrationException;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.launcher.DeploymentInitException;
import org.mule.module.launcher.DeploymentService;

/**
 *
 */
public class PriviledgedMuleApplication extends DefaultMuleApplication
{

    public static final String REGISTRY_KEY_DEPLOYMENT_SERVICE = "_deploymentService";

    protected DeploymentService deploymentService;

    protected PriviledgedMuleApplication(String appName)
    {
        super(appName);
    }

    @Override
    public void init()
    {
        if (this.deploymentService == null)
        {
            final String msg = String.format("Deployment service ref wasn't provided for privileged app '%s'", getAppName());
            throw new DeploymentInitException(MessageFactory.createStaticMessage(msg));
        }

        super.init();
        try
        {
            if (getDescriptor().isPriviledged())
            {
                getMuleContext().getRegistry().registerObject(REGISTRY_KEY_DEPLOYMENT_SERVICE, deploymentService);
            }
        }
        catch (RegistrationException e)
        {
            final String msg = String.format("Failed to init a privileged app: [%s]", getDescriptor().getAppName());
            throw new DeploymentInitException(MessageFactory.createStaticMessage(msg), e);
        }
    }

    public void setDeploymentService(DeploymentService deploymentService)
    {
        this.deploymentService = deploymentService;
    }
}
