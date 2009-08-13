/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.integration;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class JmsReplyToPropertyTestCase extends AbstractJmsFunctionalTestCase
{
    public JmsReplyToPropertyTestCase(JmsVendorConfiguration config)
    {
        super(config);
    }

    @Override
    protected String getConfigResources()
    {
        return "integration/jms-replyto-property.xml";
    }

    @Test
    public void testReplyTo() throws Exception
    {
        MuleClient client = new MuleClient();
        Map<String, String> props = new HashMap<String, String>();
        props.put("JMSReplyTo", "middle");
        client.dispatch("in", DEFAULT_INPUT_MESSAGE, props);

        // Check that the property is still on the outbound message
        MuleMessage output = client.request("out", 2000);
        assertTrue(output.getProperty("JMSReplyTo").toString().contains("middle"));
        
        // Check that the reply message was generated
        output = client.request("middle", 2000);
        assertNotNull(output);
        assertEquals(DEFAULT_OUTPUT_MESSAGE, output.getPayload());
    }
}
