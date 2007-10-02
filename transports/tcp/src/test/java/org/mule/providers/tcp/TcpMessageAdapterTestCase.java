/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.UMOMessageAdapter;

public class TcpMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{
    public Object getValidMessage() throws Exception
    {
        return "Hello".getBytes();
    }

    public void testMessageRetrieval() throws Exception
    {
        // TODO SF do we really need to remove test, since now we can handle Objects
        // so there are no invalid types
    }

    public UMOMessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new TcpMessageAdapter(payload);
    }
}
