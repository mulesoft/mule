/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import org.mule.api.component.JavaComponent;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.registry.RegistrationException;
import org.mule.api.service.Service;
import org.mule.component.AbstractJavaComponent;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * A base tast case for tests that initialize Mule using a configuration file. The
 * default configuration builder used is SpringXmlConfigurationBuilder. To use this
 * test case, ensure you have the mule-modules-builders JAR file on your classpath. 
 * To use a different builder, just overload the <code>getBuilder()</code> method
 * of this class to return the type of builder you want to use with your test. 
 */
public abstract class FunctionalTestCase extends AbstractMuleTestCase
{
    public FunctionalTestCase()
    {
        super();
        // A functional test case that starts up the management context by default.
        setStartContext(true);
    }
    
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        return new SpringXmlConfigurationBuilder(getConfigurationResources());
    }

    //Delegate to an abstract method to ensure that FunctionalTestCases know they need to pass in config resources
    protected String getConfigurationResources()
    {
        return getConfigResources();
    }

    protected abstract String getConfigResources();
    
    protected Object getComponent(String serviceName) throws Exception
    {
        Service service = muleContext.getRegistry().lookupService(serviceName);
        if (service != null)
        {
            return getComponent(service);
        }
        else
        {
            throw new RegistrationException("Service " + serviceName + " not found in Registry");
        }
    }
    
    protected Object getComponent(Service service) throws Exception
    {
        if (service.getComponent() instanceof JavaComponent)
        {
            return ((AbstractJavaComponent) service.getComponent()).getObjectFactory().getInstance();
        }
        else
        {
            fail("Componnent is not a JavaComponent and therefore has no component object instance");
            return null;
        }
    }

    protected String loadResourceAsString(String name) throws IOException
    {
        return IOUtils.getResourceAsString(name, getClass());
    }

    protected InputStream loadResource(String name) throws IOException
    {
        return IOUtils.getResourceAsStream(name, getClass());
    }
}
