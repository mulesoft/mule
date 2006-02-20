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
 * Test by reading characters from a fixed string buffer instead of
 * a tcp port.
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
        System.out.println(new String(result));
        assertNotNull(result);
        assertTrue(new String(result).trim().equals(msgData));

        result = xmp.read(bais);
        assertNull(result);
    }

    public void testMultipleMessages() throws Exception
    {
        String[] msgData = {
                "<?xml version=\"1.0\"?><data>hello</data>",
                "<?xml version=\"1.0\"?><data>goodbye</data>"
        };

        ByteArrayInputStream bais = new ByteArrayInputStream(
                (msgData[0] + msgData[1]).getBytes());

        byte[] result;
        for (int i = 0; i < 2; i++) {
            result = xmp.read(bais);
            System.out.println(new String(result));
            assertNotNull(result);
            assertTrue(new String(result).trim().equals(msgData[i]));
        }

        result = xmp.read(bais);
        assertNull(result);
    }
}
