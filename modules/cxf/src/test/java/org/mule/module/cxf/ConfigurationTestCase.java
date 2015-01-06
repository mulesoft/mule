/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
            {ConfigVariant.FLOW, "configuration-conf-flow.xml"},
            {ConfigVariant.FLOW, "configuration-conf-flow-httpn.xml"}
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
        FlowConfiguringMessageProcessor processor =
            muleContext.getRegistry().lookupObjects(FlowConfiguringMessageProcessor.class).iterator().next();
        List<Interceptor<? extends Message>> inInterceptors =
            ((ProxyServiceFactoryBean) processor.getMessageProcessorBuilder()).getInInterceptors();
        assertEquals(muleContext.getRegistry().get("foo1"), inInterceptors.get(0));
        assertEquals(muleContext.getRegistry().get("foo3"), inInterceptors.get(1));
    }
}
