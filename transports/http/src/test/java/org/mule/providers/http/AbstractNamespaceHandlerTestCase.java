/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.tck.FunctionalTestCase;

public abstract class AbstractNamespaceHandlerTestCase extends FunctionalTestCase
{

    private String prefix;

    protected AbstractNamespaceHandlerTestCase(String prefix)
    {
        this.prefix = prefix;
    }

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