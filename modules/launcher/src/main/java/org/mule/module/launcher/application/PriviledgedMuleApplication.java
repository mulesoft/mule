package org.mule.module.launcher.application;

import org.mule.api.registry.RegistrationException;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.launcher.DeploymentInitException;

/**
 *
 */
public class PriviledgedMuleApplication extends DefaultMuleApplication
{

    protected PriviledgedMuleApplication(String appName)
    {
        super(appName);
    }

    @Override
    public void init()
    {
        super.init();
        try
        {
            if (getDescriptor().isPriviledged())
            {
                // TODO implement
                getMuleContext().getRegistry().registerObject("deploymentService", "TODO");
            }
        }
        catch (RegistrationException e)
        {
            final String msg = String.format("Failed to init a priviledged app[%s]", getDescriptor().getAppName());
            throw new DeploymentInitException(MessageFactory.createStaticMessage(msg), e);
        }
    }
}
