/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

import org.mule.tck.junit4.FunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractNamespaceHandlerTestCase extends FunctionalTestCase
{

    private String prefix;

    protected AbstractNamespaceHandlerTestCase(String prefix)
    {
        this.prefix = prefix;
    }

    @Override
    protected String getConfigResources()
    {
        return prefix + "-namespace-config.xml";
    }

    protected void testBasicProperties(HttpConnector connector)
    {
        assertNotNull(connector);

        assertEquals(1234, connector.getClientSoTimeout());
        assertEquals("netscape", connector.getCookieSpec());
        assertEquals("bcd", connector.getProxyHostname());
        assertEquals("cde", connector.getProxyPassword());
        assertEquals(2345, connector.getProxyPort());
        assertEquals("def", connector.getProxyUsername());
        assertEquals(34, connector.getReceiveBacklog());
        assertEquals(4567, connector.getReceiveBufferSize());
        assertEquals(5678, connector.getSendBufferSize());
        assertEquals(6789, connector.getSocketSoLinger());
        assertEquals(7890, connector.getServerSoTimeout());
        assertEquals(true, connector.isEnableCookies());
        assertEquals(true, connector.isKeepAlive());
        assertEquals(true, connector.isKeepSendSocketOpen());
        assertEquals(true, connector.isSendTcpNoDelay());
        assertEquals(false, connector.isValidateConnections());
    }

}
