/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.jaxws;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class JettyTestCase extends AbstractServiceAndFlowTestCase
{
    private static final Bus defaultBus = BusFactory.getDefaultBus();

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public JettyTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "jetty-conf-service.xml"},
            {ConfigVariant.FLOW, "jetty-conf-flow.xml"}
        });
    }

    @BeforeClass
    public static void setUpDefaultBus()
    {
        BusFactory.setDefaultBus(null);
    }

    @AfterClass
    public static void restoreDefaultBus()
    {
        BusFactory.setDefaultBus(defaultBus);
    }

    @Test
    public void testClientWithMuleClient() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("operation", "greetMe");
        MuleMessage result = client.send("clientEndpoint", "Dan", props);
        assertEquals("Hello Dan", result.getPayload());

        final GreeterImpl impl = getGreeter();

        Prober prober = new PollingProber(5000, 100);
        prober.check(new GreeterNotNull(impl));

        assertEquals(1, impl.getInvocationCount());
    }

    private GreeterImpl getGreeter() throws Exception
    {
        Object instance = getComponent("greeterService");

        return (GreeterImpl) instance;
    }
}
