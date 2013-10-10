/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transformer.response;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class ResponseTransformerScenariosTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort dynamicPort3 = new DynamicPort("port3");

    @Rule
    public DynamicPort dynamicPort4 = new DynamicPort("port4");

    public ResponseTransformerScenariosTestCase()
    {
        setDisposeContextPerClass(true);
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/transformer/response/response-transformer-scenarios.xml";
    }

    // ***** RESPONSE ENDPONTS ON INBOUND ENDPOINTS USED FOR SYNC RESPONSE AFTER ROUTING *****
    // Applied by DefaultInternalMessageListener

    // TODO Not sure how to implement with axis

    // public void testAxisSyncResponseTransformer() throws Exception
    // {
    // MuleClient client = new MuleClient();
    // MuleMessage message = client.send("axis:http://localhost:4445/services/AxisSync?method=echo",
    // "request",
    // null);
    // assertNotNull(message);
    // assertEquals("request" + "customResponse", message.getPayloadAsString());
    // }

    /**
     * make maven happy
     */
    @Test
    public void testDummy()
    {
    }

}
