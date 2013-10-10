/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Tests the correct propagation of the correlation id property within the JMS transport. This test is related to MULE-6577.
 */
public class JmsCorrelationIdPropagationTestCase extends AbstractJmsFunctionalTestCase
{
    private final static String CUSTOM_CORRELATION_ID = "custom-cid";
    private final static int RECEIVE_TIMEOUT = 2 * AbstractMuleContextTestCase.RECEIVE_TIMEOUT;

    @Override
    protected String getConfigResources()
    {
        return "integration/jms-correlation-id-propagation.xml";
    }

    @Test
    public void testCorrelationIdPropagation() throws MuleException
    {
        MuleClient muleClient = new MuleClient(muleContext);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(MuleProperties.MULE_CORRELATION_ID_PROPERTY, CUSTOM_CORRELATION_ID);

        muleClient.dispatch("vm://in", TEST_MESSAGE, properties);

        MuleMessage response = muleClient.request("vm://out", RECEIVE_TIMEOUT);

        assertNotNull(response);
        assertEquals(getCustomCorrelationId(), response.getOutboundProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY));
    }

    /**
     * Returns the custom correlation id.
     *
     * @return The custom correlation id.
     */
    protected String getCustomCorrelationId()
    {
        return CUSTOM_CORRELATION_ID;
    }
}
