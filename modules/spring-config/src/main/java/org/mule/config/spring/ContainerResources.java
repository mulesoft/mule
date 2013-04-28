package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;

import org.springframework.context.ApplicationContext;

/**
 *
 */
public class ContainerResources
{

    private static ApplicationContext serverContext;

    public synchronized static ApplicationContext getServerResourcesContext()
    {
        if (serverContext == null)
        {
            try
            {
                MuleContext muleContext = new DefaultMuleContextFactory().createMuleContext();
                serverContext = new SpringXmlConfigurationBuilder("mule-server-config.xml").doConfigure2(muleContext);
                try
                {
                    muleContext.start();
                }
                catch (Exception e)
                {
                    //NOTHING TO DO
                }
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(e);
            }
        }
        return serverContext;
    }
}
