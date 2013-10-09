/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.jaxws;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import java.util.HashMap;
import java.util.Map;

import org.apache.hello_world_soap_http.GreeterImpl;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClientTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    private Prober prober = new PollingProber(5000, 100);

    @Override
    protected String getConfigResources()
    {
        return "jaxws-client-conf.xml";
    }

    @Test
    public void testGeneratedClientWithQuartz() throws Exception
    {
        final GreeterImpl impl = getGreeter();
        prober.check(new GreeterNotNull(impl));

        assertEquals(1, impl.getInvocationCount());
    }

    @Test
    public void testClientWithMuleClient() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("operation", "greetMe");
        MuleMessage result = client.send("clientEndpoint", "Dan", props);
        assertEquals("Hello Dan", result.getPayload());

        final GreeterImpl impl = getGreeter();
        prober.check(new GreeterNotNull(impl));

        assertEquals(2, impl.getInvocationCount());
    }

    private GreeterImpl getGreeter() throws Exception
    {
        Object instance = getComponent("greeterService");

        return (GreeterImpl) instance;
    }

}
