/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.MessageReceiver;
import org.mule.construct.Flow;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Orange;

import org.junit.Test;

public abstract class AbstractMessageReceiverTestCase extends AbstractMuleContextTestCase
{
    protected Flow flow;
    protected InboundEndpoint endpoint;

    protected void doSetUp() throws Exception
    {
        flow = getTestFlow("orange", Orange.class);
        endpoint = getEndpoint();
    }

    @Test
    public void testCreate() throws Exception
    {
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
