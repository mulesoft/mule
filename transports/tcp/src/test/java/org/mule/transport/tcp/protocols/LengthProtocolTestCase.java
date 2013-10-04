/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
