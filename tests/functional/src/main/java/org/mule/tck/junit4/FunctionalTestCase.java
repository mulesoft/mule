/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import static org.junit.Assert.fail;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.component.JavaComponent;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.Schedulers;
import org.mule.api.service.Service;
import org.mule.component.AbstractJavaComponent;
import org.mule.config.i18n.MessageFactory;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.construct.AbstractPipeline;
import org.mule.construct.Flow;
import org.mule.construct.SimpleService;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.junit.After;
import org.junit.Assert;

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

    /**
     * @return
     * @deprecated use getConfigFile instead.
     */
    @Deprecated
    protected String getConfigResources()
    {
        return null;
    }


    @Override
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        String configResources = getConfigResources();
        if (configResources != null)
        {
            return new SpringXmlConfigurationBuilder(configResources);
        }
        configResources = getConfigFile();
        if (configResources != null)
        {
            if (configResources.contains(","))
            {
                throw new RuntimeException("Do not use this method when the config is composed of several files. Use getConfigFiles method instead.");
            }
            return new SpringXmlConfigurationBuilder(configResources);
        }
        String[] multipleConfigResources = getConfigFiles();
        return new SpringXmlConfigurationBuilder(multipleConfigResources);
    }

    /**
     * @return a single file that defines a mule application configuration
     */
    protected String getConfigFile()
    {
        return null;
    }

    /**
     * @return a several files that define a mule application configuration
     */
    protected String[] getConfigFiles()
    {
        return null;
    }

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
                if (processor instanceof Component)
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

    /**
     * Tests the given flow with a one-way message exchange pattern
     * 
     * @param flowName the name of the flow to be executed
     * @throws Exception
     */
    protected void testFlow(String flowName) throws Exception
    {
        testFlow(flowName, getTestEvent("data", MessageExchangePattern.ONE_WAY));
    }

    /**
     * Looks up the given flow in the registry and processes it with the given event.
     * A flow asserting is then executed by calling {@link
     * org.mule.tck.functional.FlowAssert.verify(String)}
     * 
     * @param flowName the name of the flow to be executed
     * @param event the event ot execute with
     * @throws Exception
     */
    protected void testFlow(String flowName, MuleEvent event) throws Exception
    {
        Flow flow = this.lookupFlowConstruct(flowName);
        flow.process(event);
        FlowAssert.verify(flowName);
    }

    /**
     * Runs the given flow with a default event
     * 
     * @param flowName the name of the flow to be executed
     * @return the resulting <code>MuleEvent</code>
     * @throws Exception
     */
    protected MuleEvent runFlow(String flowName) throws Exception
    {
        return this.runFlow(flowName, null);
    }

    /**
     * Executes the given flow with a default message carrying the payload
     * 
     * @param flowName the name of the flow to be executed
     * @param payload the payload to use int he message
     * @return the resulting <code>MuleEvent</code>
     * @throws Exception
     */
    protected <T> MuleEvent runFlow(String flowName, T payload) throws Exception
    {
        Flow flow = lookupFlowConstruct(flowName);
        return flow.process(getTestEvent(payload));
    }

    /**
     * Run the flow specified by name and assert equality on the expected output
     * 
     * @param flowName The name of the flow to run
     * @param expect The expected output
     */
    protected <T> void runFlowAndExpect(String flowName, T expect) throws Exception
    {
        Assert.assertEquals(expect, this.runFlow(flowName).getMessage().getPayload());
    }

    /**
     * Runs the given flow and asserts for property name in the outbound scope to
     * match the expected value
     * 
     * @param flowName the name of the flow to be executed
     * @param propertyName the name of the property to test
     * @param expect the expected value
     * @throws Exception
     */
    protected <T> void runFlowAndExpectProperty(String flowName, String propertyName, T expect)
        throws Exception
    {
        Flow flow = lookupFlowConstruct(flowName);
        MuleEvent event = getTestEvent(null);
        MuleEvent responseEvent = flow.process(event);

        Assert.assertEquals(expect, responseEvent.getMessage().getOutboundProperty(propertyName));
    }

    /**
     * Run the flow specified by name using the specified payload and assert equality
     * on the expected output
     * 
     * @param flowName The name of the flow to run
     * @param expect The expected output
     * @param payload The payload of the input event
     */
    protected <T, U> void runFlowWithPayloadAndExpect(String flowName, T expect, U payload) throws Exception
    {
        Assert.assertEquals(expect, this.runFlow(flowName, payload).getMessage().getPayload());
    }

    /**
     * Retrieve a flow by name from the registry
     * 
     * @param name Name of the flow to retrieve
     */
    protected Flow lookupFlowConstruct(String name)
    {
        return (Flow) muleContext.getRegistry().lookupFlowConstruct(name);
    }

    @After
    public final void clearFlowAssertions() throws Exception
    {
        FlowAssert.reset();
    }

    protected void stopFlowSchedulers(String flowName) throws MuleException
    {
        final Collection<Scheduler> schedulers = muleContext.getRegistry().lookupScheduler(Schedulers.flowConstructPollingSchedulers(flowName));
        for (final Scheduler scheduler : schedulers)
        {
            scheduler.stop();
        }
    }

    protected SubflowInterceptingChainLifecycleWrapper getSubFlow(String subflowName)
    {
        return (SubflowInterceptingChainLifecycleWrapper) muleContext.getRegistry().lookupObject(subflowName);
    }

    protected void runSchedulersOnce(String flowConstructName) throws Exception
    {
        final Collection<Scheduler> schedulers = muleContext.getRegistry().lookupScheduler(Schedulers.flowConstructPollingSchedulers(flowConstructName));

        for (final Scheduler scheduler : schedulers)
        {
            scheduler.schedule();
        }
    }

}
