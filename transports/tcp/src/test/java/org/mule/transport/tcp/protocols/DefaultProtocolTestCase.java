/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.protocols;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.tcp.TcpProtocol;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

@SmallTest
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

    protected DefaultProtocolTestCase(TcpProtocol protocol, int expectedLength)
    {
        this.protocol = protocol;
        this.expectedLength = expectedLength;
    }

    @Test
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
