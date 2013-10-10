/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.cxf.config.FlowConfiguringMessageProcessor;
import org.mule.module.cxf.config.ProxyServiceFactoryBean;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.message.Message;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ConfigurationTestCase extends AbstractServiceAndFlowTestCase
{
    public ConfigurationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "configuration-conf-service.xml"},
            {ConfigVariant.FLOW, "configuration-conf-flow.xml"}
        });
    }      
    
    @Test
    public void testBusConfiguration() throws Exception
    {
        CxfConfiguration config = muleContext.getRegistry().get("cxf");

        Bus cxfBus = config.getCxfBus();
        boolean found = false;
        for (Interceptor<? extends Message> i : cxfBus.getInInterceptors())
        {
            if (i instanceof LoggingInInterceptor)
            {
                found = true;
                break;
            }
        }

        assertTrue("Did not find logging interceptor.", found);
    }

    @Test
    public void testSpringRefs() throws Exception
    {
        InboundEndpoint endpoint = muleContext.getRegistry().get("clientEndpoint");
        FlowConfiguringMessageProcessor processor = (FlowConfiguringMessageProcessor) endpoint.getMessageProcessors().get(0);
        List<Interceptor<? extends Message>> inInterceptors =
            ((ProxyServiceFactoryBean) processor.getMessageProcessorBuilder()).getInInterceptors();
        assertEquals(muleContext.getRegistry().get("foo1"), inInterceptors.get(0));
        assertEquals(muleContext.getRegistry().get("foo3"), inInterceptors.get(1));
    }
}
