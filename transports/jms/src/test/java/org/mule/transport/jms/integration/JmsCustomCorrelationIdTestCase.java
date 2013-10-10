/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
