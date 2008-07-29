/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms;

import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;
import org.mule.tck.testmodels.fruit.Orange;

public class MultiConsumerJmsMessageReceiverTestCase extends JmsMessageReceiverTestCase
{
    public void testReceive() throws Exception
    {
        MultiConsumerJmsMessageReceiver receiver = (MultiConsumerJmsMessageReceiver)getMessageReceiver();
        assertNotNull(receiver.getService());
        assertNotNull(receiver.getConnector());
        assertNotNull(receiver.getEndpoint());
    }

    public MessageReceiver getMessageReceiver() throws Exception
    {
        Service service = getTestService("orange", Orange.class);
        return new MultiConsumerJmsMessageReceiver(endpoint.getConnector(), service, endpoint);
    }
}
