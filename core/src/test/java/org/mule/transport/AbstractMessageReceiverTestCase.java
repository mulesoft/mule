/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Orange;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public abstract class AbstractMessageReceiverTestCase extends AbstractMuleContextTestCase
{
    protected Service service;
    protected InboundEndpoint endpoint;

    protected void doSetUp() throws Exception
    {
        service = getTestService("orange", Orange.class);
        endpoint = getEndpoint();
    }

    @Test
    public void testCreate() throws Exception
    {
        Service service = getTestService("orange", Orange.class);
        InboundEndpoint endpoint = getTestInboundEndpoint("Test");
        MessageReceiver receiver = getMessageReceiver();

        assertNotNull(receiver.getEndpoint());

        try
        {
            receiver.setEndpoint(null);
            fail("Provider cannot be set to null");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }

        receiver.setEndpoint(endpoint);
        assertNotNull(receiver.getEndpoint());

        receiver.dispose();
    }

    public abstract MessageReceiver getMessageReceiver() throws Exception;

    /**
     * Implementations of this method should ensure that the correct connector is set
     * on the endpoint
     * 
     * @throws Exception
     */
    public abstract InboundEndpoint getEndpoint() throws Exception;
}
