/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.providers.tcp.protocols;

import org.mule.tck.AbstractMuleTestCase;

import java.io.ByteArrayInputStream;

/**
 * Test by reading characters from a fixed string buffer instead of a tcp port.
 * 
 * @author <a href="mailto:rlucente@xecu.net">Rich Lucente</a>
 * @version $Revision$
 */
public class XmlMessageProtocolTestCase extends AbstractMuleTestCase
{
    private XmlMessageProtocol xmp;

    protected void doSetUp() throws Exception
    {
        xmp = new XmlMessageProtocol();
    }

    protected void doTearDown() throws Exception
    {
        xmp = null;
    }

    public void testSingleMessage() throws Exception
    {
        String msgData = "<?xml version=\"1.0\"?><data>hello</data>";

        ByteArrayInputStream bais = new ByteArrayInputStream(msgData.getBytes());

        byte[] result = xmp.read(bais);
        assertNotNull(result);
        assertEquals(msgData, new String(result));

        result = xmp.read(bais);
        assertNull(result);
    }

    public void testTwoMessages() throws Exception
    {
        String[] msgData = {
            "<?xml version=\"1.0\"?><data>hello</data>",
            "<?xml version=\"1.0\"?><data>goodbye</data>"
        };

        ByteArrayInputStream bais = new ByteArrayInputStream((msgData[0] + msgData[1]).getBytes());

        byte[] result = xmp.read(bais);
        assertNotNull(result);
        assertEquals(msgData[0], new String(result));

        result = xmp.read(bais);
        assertNotNull(result);
        assertEquals(msgData[1], new String(result));

        result = xmp.read(bais);
        assertNull(result);
    }

    public void testMultipleMessages() throws Exception
    {
        String[] msgData = {
            "<?xml version=\"1.0\"?><data>1</data>",
            "<?xml version=\"1.0\"?><data>22</data>",
            "<?xml version=\"1.0\"?><data>333</data>",
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
            "<?xml version=\"1.0\"?><data>fffffffffffffff</data>"
        };

        StringBuffer allMsgData = new StringBuffer();

        for (int i = 0; i < msgData.length; i++) {
            allMsgData.append(msgData[i]);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(allMsgData.toString().getBytes());

        byte[] result;
        
        for (int i = 0; i < msgData.length; i++) {
            result = xmp.read(bais);
            assertNotNull(result);
            assertEquals(msgData[i], new String(result));
        }

        result = xmp.read(bais);
        assertNull(result);
    }

}
