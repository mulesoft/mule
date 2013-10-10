/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis.extensions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.axis.AxisEngine;
import org.apache.axis.ConfigurationException;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.configuration.SimpleProvider;

/**
 * <code>MuleConfigProvider</code> is needed because the Simple Provider does not
 * list services in the defaultConfiguration.
 */
public class MuleConfigProvider extends SimpleProvider
{
    private EngineConfiguration engineConfiguration;

    public MuleConfigProvider(EngineConfiguration engineConfiguration)
    {
        super(engineConfiguration);
        this.engineConfiguration = engineConfiguration;
    }

    /**
     * Configure an AxisEngine. Right now just calls the default configuration if
     * there is one, since we don't do anything special.
     */
    public void configureEngine(AxisEngine engine) throws ConfigurationException
    {
        synchronized (this)
        {
            engineConfiguration.configureEngine(engine);
            super.configureEngine(engine);
        }
    }

    public Iterator getAxisDeployedServices() throws ConfigurationException
    {
        return engineConfiguration.getDeployedServices();
    }

    public Iterator getAllDeployedServices() throws ConfigurationException
    {
        List services = new ArrayList();
        Iterator iter = engineConfiguration.getDeployedServices();
        while (iter.hasNext())
        {
            services.add(iter.next());
        }
        iter = super.getDeployedServices();
        while (iter.hasNext())
        {
            services.add(iter.next());
        }
        return services.iterator();
    }
}
