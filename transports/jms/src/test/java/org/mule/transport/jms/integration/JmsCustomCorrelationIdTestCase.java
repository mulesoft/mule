/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JmsCustomCorrelationIdTestCase extends AbstractJmsFunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "integration/jms-custom-correlation.xml";
    }

    @Test
    public void testExplicitReplyToAsyncSet() throws MuleException
    {
        MuleClient muleClient = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("customCorrelation", "abcdefghij");
        MuleMessage response = muleClient.send("vm://in4", TEST_MESSAGE, props);
        // We get the original message back, not the result from the remote component
        assertEquals(TEST_MESSAGE + " TestService1", response.getPayload());
    }
}
