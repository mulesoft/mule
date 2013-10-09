/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.echo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * @author alejandrosequeira
 *
 */
public class EchoTestCase extends FunctionalTestCase
{

    private static String MESSAGE = "message";

    @Override
    protected String getConfigResources()
    {
        return "adding-logging-to-a-flow.xml";
    }

    @Test
    public void httpGetToFlowUrlEchoesSentMessage() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, String> props = new HashMap<String, String>();
        props.put("http.method", "GET");
        MuleMessage result = client.send("http://localhost:8084/" + MESSAGE, "", props);
        assertNotNull(result);
        assertFalse(result.getPayload() instanceof NullPayload);
        assertEquals("/" + MESSAGE, result.getPayloadAsString());
    }

}
