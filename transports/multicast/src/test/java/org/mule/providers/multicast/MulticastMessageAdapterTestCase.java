/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.multicast;

import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.UMOMessageAdapter;

import java.net.DatagramPacket;

public class MulticastMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{

    public Object getValidMessage() throws Exception
    {
        return new DatagramPacket("Hello".getBytes(), 5);
    }

    public UMOMessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new MulticastMessageAdapter(payload);
    }

    public void testMessageRetrieval() throws Exception
    {
        Object message = getValidMessage();
        UMOMessageAdapter adapter = createAdapter(message);

        assertEquals(new String(((DatagramPacket)message).getData()), adapter.getPayloadAsString());
        byte[] bytes = adapter.getPayloadAsBytes();
        assertNotNull(bytes);

        String stringMessage = adapter.getPayloadAsString();
        assertNotNull(stringMessage);

        assertNotNull(adapter.getPayload());

        try
        {
            adapter = createAdapter(getInvalidMessage());
            fail("Message adapter should throw exception if an invalid messgae is set");
        }
        catch (Exception e)
        {
            // expected
        }
    }

}
