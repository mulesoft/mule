/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Orange;

public abstract class AbstractMessageReceiverTestCase extends AbstractMuleTestCase
{
    protected Service service;
    protected ImmutableEndpoint endpoint;

    protected void doSetUp() throws Exception
    {
        service = getTestService("orange", Orange.class);
        endpoint = getEndpoint();
    }

    public void testCreate() throws Exception
    {
        Service service = getTestService("orange", Orange.class);
        ImmutableEndpoint endpoint = getTestOutboundEndpoint("Test");
        MessageReceiver receiver = getMessageReceiver();

        assertNotNull(receiver.getEndpoint());
        assertNotNull(receiver.getConnector());

        try
        {
            receiver.setEndpoint(null);
            fail("Provider cannot be set to null");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }

        try
        {
            receiver.setService(null);
            fail("service cannot be set to null");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }

        receiver.setService(service);
        assertNotNull(receiver.getService());
        receiver.setEndpoint(endpoint);
        assertNotNull(receiver.getEndpoint());

        receiver.dispose();
    }

    public abstract MessageReceiver getMessageReceiver() throws Exception;

    /**
     * Implementations of this method should ensure that the correct connector is set
     * on the endpoint
     * 
     * @return
     * @throws Exception
     */
    public abstract ImmutableEndpoint getEndpoint() throws Exception;
}
