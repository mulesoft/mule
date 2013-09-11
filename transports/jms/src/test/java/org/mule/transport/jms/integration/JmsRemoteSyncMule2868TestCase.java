/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;

import org.junit.Test;

public class JmsRemoteSyncMule2868TestCase extends AbstractJmsFunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "integration/jms-remote-sync-mule2868.xml";
    }

    @Test
    public void testMule2868() throws MuleException
    {
        MuleClient muleClient = muleContext.getClient();
        MuleMessage response = muleClient.send("vm://in", "test", null);
        assertEquals("test Received", response.getPayload());
    }
}
