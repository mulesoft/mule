/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.gs;

import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.UMOMessageAdapter;

/**
 * @version $Revision$
 */
public class JiniMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{

    public void doTestMessageEqualsPayload(Object message, Object payload) throws Exception
    {
        if (message instanceof JiniMessage && payload instanceof String)
        {
            assertEquals(((JiniMessage)message).getPayload(), payload);
        }
        else
        {
            fail("message must be a JiniMessage and payload must be a String");
        }
    }

    public Object getValidMessage() throws Exception
    {
        return new JiniMessage(null, "hello");
    }

    public Object getInvalidMessage()
    {
        return null;
    }

    public UMOMessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new JiniMessageAdapter(payload);
    }

}
