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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
        MuleClient muleClient = new MuleClient(muleContext);
        MuleMessage response = muleClient.send("vm://in", "test", null);
        assertEquals("test Received", response.getPayload());
    }

}
