/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleProperties;
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
    protected String getConfigFile()
    {
        return "integration/jms-correlation-id-propagation.xml";
    }

    @Test
    public void testCorrelationIdPropagation() throws MuleException
    {
        MuleClient muleClient = muleContext.getClient();

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
