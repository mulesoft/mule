/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.protocols;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class XmlMessageEOFProtocolTestCase extends XmlMessageProtocolTestCase
{

    @Override
    public void doSetUp()
    {
        setProtocol(new XmlMessageEOFProtocol());
    }

    @Override
    public void testSlowStream() throws Exception
    {
        String msgData = "<?xml version=\"1.0\"?><data>hello</data>";

        SlowInputStream bais = new SlowInputStream(msgData.getBytes());

        byte[] result = read(bais);
        assertNotNull(result);
        // hurray!  get everything
        assertEquals(msgData, new String(result));
    }

}
