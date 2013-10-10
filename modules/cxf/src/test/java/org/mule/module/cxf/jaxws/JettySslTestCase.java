/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.jaxws;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

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
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

public class JettySslTestCase extends AbstractServiceAndFlowTestCase
{
    private static final Bus defaultBus = BusFactory.getDefaultBus();

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public JettySslTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.SERVICE, "jetty-ssl-conf-service.xml"},
                {ConfigVariant.FLOW, "jetty-ssl-conf-flow.xml"}
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

        GreeterImpl impl = getGreeter();
        assertEquals(1, impl.getInvocationCount());
    }

    private GreeterImpl getGreeter() throws Exception
    {
        return (GreeterImpl) getComponent("greeterService");
    }


}
