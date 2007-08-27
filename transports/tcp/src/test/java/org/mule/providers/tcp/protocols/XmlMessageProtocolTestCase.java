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

import org.mule.tck.AbstractMuleTestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Test by reading characters from a fixed StringBuffer instead of a TCP port.
 */
public class XmlMessageProtocolTestCase extends AbstractMuleTestCase
{
    private XmlMessageProtocol xmp;

    protected void setProtocol(XmlMessageProtocol xmp)
    {
        this.xmp = xmp;
    }

    protected byte[] read(InputStream is) throws IOException
    {
        return(byte[]) xmp.read(is);
    }

    protected void doSetUp() throws Exception
    {
        setProtocol(new XmlMessageProtocol());
    }

    protected void doTearDown() throws Exception
    {
        xmp = null;
    }

    public void testSingleMessage() throws Exception
    {
        String msgData = "<?xml version=\"1.0\"?><data>hello</data>";

        ByteArrayInputStream bais = new ByteArrayInputStream(msgData.getBytes());

        byte[] result = read(bais);
        assertNotNull(result);
        assertEquals(msgData, new String(result));

        assertNull(read(bais));
    }

    public void testTwoMessages() throws Exception
    {
        String[] msgData = {"<?xml version=\"1.0\"?><data>hello</data>",
            "<?xml version=\"1.0\"?><data>goodbye</data>"};

        ByteArrayInputStream bais = new ByteArrayInputStream((msgData[0] + msgData[1]).getBytes());

        byte[] result = read(bais);
        assertNotNull(result);
        assertEquals(msgData[0], new String(result));

        result = read(bais);
        assertNotNull(result);
        assertEquals(msgData[1], new String(result));

        assertNull(read(bais));
    }

    public void testMultipleMessages() throws Exception
    {
        String[] msgData = {"<?xml version=\"1.0\"?><data>1</data>",
            "<?xml version=\"1.0\"?><data>22</data>", "<?xml version=\"1.0\"?><data>333</data>",
            "<?xml version=\"1.0\"?><data>4444</data>",
            "<?xml version=\"1.0\"?><data>55555</data>",
            "<?xml version=\"1.0\"?><data>666666</data>",
            "<?xml version=\"1.0\"?><data>7777777</data>",
            "<?xml version=\"1.0\"?><data>88888888</data>",
            "<?xml version=\"1.0\"?><data>999999999</data>",
            "<?xml version=\"1.0\"?><data>aaaaaaaaaa</data>",
            "<?xml version=\"1.0\"?><data>bbbbbbbbbbb</data>",
            "<?xml version=\"1.0\"?><data>cccccccccccc</data>",
            "<?xml version=\"1.0\"?><data>ddddddddddddd</data>",
            "<?xml version=\"1.0\"?><data>eeeeeeeeeeeeee</data>",
            "<?xml version=\"1.0\"?><data>fffffffffffffff</data>"};

        StringBuffer allMsgData = new StringBuffer();

        for (int i = 0; i < msgData.length; i++)
        {
            allMsgData.append(msgData[i]);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(allMsgData.toString().getBytes());

        byte[] result;

        for (int i = 0; i < msgData.length; i++)
        {
            result = read(bais);
            assertNotNull(result);
            assertEquals(msgData[i], new String(result));
        }

        assertNull(read(bais));
    }

    public void testSlowStream() throws Exception
    {
        String msgData = "<?xml version=\"1.0\"?><data>hello</data>";

        SlowInputStream bais = new SlowInputStream(msgData.getBytes());

        byte[] result = read(bais);
        assertNotNull(result);
        // only get the first character!  use XmlMessageEOFProtocol instead
        assertEquals(msgData.substring(0, 1), new String(result));
    }

}
