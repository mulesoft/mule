/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.protocols;

import org.mule.providers.tcp.TcpProtocol;
import org.mule.tck.AbstractMuleTestCase;

public class DefaultProtocolTestCase extends AbstractMuleTestCase
{

    private TcpProtocol protocol;
    private int expectedLength;

    public DefaultProtocolTestCase()
    {
        // for old (full buffer) condition in DefaultProtocol
//        this(new DefaultProtocol(), 1);

        this(new DirectProtocol(), SlowInputStream.FULL_LENGTH);
    }

    public DefaultProtocolTestCase(TcpProtocol protocol, int expectedLength)
    {
        this.protocol = protocol;
        this.expectedLength = expectedLength;
    }


    public void testRead() throws Exception
    {
        byte[] result = (byte[]) protocol.read(new SlowInputStream());
        assertEquals(expectedLength, result.length);
    }

    protected TcpProtocol getProtocol()
    {
        return protocol;
    }

}
