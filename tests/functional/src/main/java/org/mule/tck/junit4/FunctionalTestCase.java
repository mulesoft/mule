/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.junit4;

import static org.junit.Assert.fail;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.component.Component;
import org.mule.api.component.JavaComponent;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.api.service.Service;
import org.mule.component.AbstractJavaComponent;
import org.mule.config.i18n.MessageFactory;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.construct.AbstractPipeline;
import org.mule.construct.Flow;
import org.mule.construct.SimpleService;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;

/**
 * A base test case for tests that initialize Mule using a configuration file. The
 * default configuration builder used is SpringXmlConfigurationBuilder. To use this
 * test case, ensure you have the mule-modules-builders JAR file on your classpath.
 * To use a different builder, just overload the <code>getBuilder()</code> method of
 * this class to return the type of builder you want to use with your test.
 */
public abstract class FunctionalTestCase extends AbstractMuleContextTestCase
{
    public FunctionalTestCase()
    {
        super();
        // A functional test case that starts up the management context by default.
        setStartContext(true);
    }

    @Override
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        return new SpringXmlConfigurationBuilder(getConfigResources());
    }

    protected abstract String getConfigResources();

    /**
     * Returns an instance of the service's component object. Note that depending on
     * the type of ObjectFactory used for the component, this may create a new
     * instance of the object. If you plan to set properties on the returned object,
     * make sure your component is declared as a singleton, otherwise this will not
     * work.
     */
    protected Object getComponent(String serviceName) throws Exception
    {
        final FlowConstruct flowConstruct = muleContext.getRegistry().lookupObject(serviceName);

        if (flowConstruct != null)
        {
            return getComponent(flowConstruct);
        }
        else
        {
            throw new RegistrationException(MessageFactory.createStaticMessage("Service " + serviceName
                                                                               + " not found in Registry"));
        }
    }

    /**
     * Returns an instance of the service's component object. Note that depending on
     * the type of ObjectFactory used for the component, this may create a new
     * instance of the object. If you plan to set properties on the returned object,
     * make sure your component is declared as a singleton, otherwise this will not
     * work.
     */
    protected Object getComponent(FlowConstruct flowConstruct) throws Exception
    {
        if (flowConstruct instanceof Service)
        {
            return getComponentObject(((Service) flowConstruct).getComponent());
        }
        else if (flowConstruct instanceof SimpleService)
        {
            return getComponentObject(((SimpleService) flowConstruct).getComponent());
        }
        else if (flowConstruct instanceof AbstractPipeline)
        {
            AbstractPipeline flow = (AbstractPipeline) flowConstruct;
            // Retrieve the first component
            for (MessageProcessor processor : flow.getMessageProcessors())
            {
                if(processor instanceof Component)
                {
                    return getComponentObject(((Component) processor));
                }
            }
        }

        throw new RegistrationException(
                MessageFactory.createStaticMessage("Can't get component from flow construct "
                                                   + flowConstruct.getName()));
    }

    /**
     * A convenience method to get a type-safe reference to the FunctionTestComponent
     *
     * @param serviceName service name as declared in the config
     * @return test component
     * @since 2.2
     * @see org.mule.tck.functional.FunctionalTestComponent
     */
    protected FunctionalTestComponent getFunctionalTestComponent(String serviceName) throws Exception
    {
        return (FunctionalTestComponent) getComponent(serviceName);
    }

    protected FlowConstruct getFlowConstruct(String flowName) throws Exception
    {
        return muleContext.getRegistry().lookupFlowConstruct(flowName);
    }

    protected String loadResourceAsString(String resourceName) throws IOException
    {
        return IOUtils.getResourceAsString(resourceName, getClass());
    }

    protected InputStream loadResource(String resourceName) throws IOException
    {
        return IOUtils.getResourceAsStream(resourceName, getClass());
    }

    private Object getComponentObject(Component component) throws Exception
    {
        if (component instanceof JavaComponent)
        {
            return ((AbstractJavaComponent) component).getObjectFactory().getInstance(muleContext);
        }
        else
        {
            fail("Component is not a JavaComponent and therefore has no component object instance");
            return null;
        }
    }

    protected void stopFlowConstruct(String flowName) throws Exception
    {
        FlowConstruct flowConstruct = getFlowConstruct(flowName);
        if (flowConstruct instanceof Service)
        {
            Service service = (Service) flowConstruct;
            service.stop();
        }
        else
        {
            Flow flow = (Flow) flowConstruct;
            flow.stop();
        }
    }
    
    protected void testFlow(String flowName) throws Exception
    {
        testFlow(flowName, getTestEvent("data", MessageExchangePattern.ONE_WAY));
    }

    protected void testFlow(String flowName, MuleEvent event) throws Exception
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);
        flow.process(event);
        FlowAssert.verify(flowName);
    }
    
    @After
    public final void clearFlowAssertions() throws Exception
    {
        FlowAssert.reset();
    }

}
