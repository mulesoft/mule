/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.protocols;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LengthProtocolTestCase extends DefaultProtocolTestCase
{

    public LengthProtocolTestCase()
    {
        super(new LengthProtocol(), 1);
    }

    @Test
    public void testFinalValue() throws Exception
    {
        byte[] result = (byte[]) getProtocol().read(new SlowInputStream());
        assertEquals((byte) SlowInputStream.PAYLOAD, result[0]);
    }

    // try also with a "generous" input stream (ie one that will return all data
    // without delay) to make sure reset logic is ok
    @Test
    public void testGenerous() throws Exception
    {
        byte[] bytes = new byte[SlowInputStream.FULL_LENGTH];
        for (int i = 0; i < SlowInputStream.FULL_LENGTH; ++i)
        {
            bytes[i] = (byte) SlowInputStream.CONTENTS[i];
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        byte[] result = (byte[]) getProtocol().read(bis);
        assertEquals((byte) SlowInputStream.PAYLOAD, result[0]);
    }

}
