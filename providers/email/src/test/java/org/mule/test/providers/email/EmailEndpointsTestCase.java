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
package org.mule.test.providers.email;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.NamedTestCase;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class EmailEndpointsTestCase extends NamedTestCase
{
    public void testPop3Url() throws Exception
    {
        MuleEndpointURI endpointUri = new MuleEndpointURI("pop3://username:password@pop3.muleumo.org");
        assertEquals("pop3", endpointUri.getScheme());
        assertEquals("username@muleumo.org", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(-1, endpointUri.getPort());
        assertEquals("pop3.muleumo.org", endpointUri.getHost());
        assertEquals("username:password", endpointUri.getUserInfo());
        assertEquals("pop3://username:password@pop3.muleumo.org", endpointUri.toString());
        assertEquals(0, endpointUri.getParams().size());
    }

    public void testSmtpUrl() throws Exception
    {
        MuleEndpointURI url = new MuleEndpointURI("smtp://username:password@smtp.muleumo.org");
        assertEquals("smtp", url.getScheme());
        assertEquals("username@muleumo.org", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(-1, url.getPort());
        assertEquals("smtp.muleumo.org", url.getHost());
        assertEquals("username:password", url.getUserInfo());
        assertEquals("smtp://username:password@smtp.muleumo.org", url.toString());
        assertEquals(0, url.getParams().size());
    }

    public void testSmtpUrlWithPort() throws Exception
    {
        MuleEndpointURI url = new MuleEndpointURI("smtp://user:password@hostname:3125");
        assertEquals("smtp", url.getScheme());
        assertEquals("user@hostname:3125", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(3125, url.getPort());
        assertEquals("hostname", url.getHost());
        assertEquals("user:password", url.getUserInfo());
        assertEquals("smtp://user:password@hostname:3125", url.toString());
        assertEquals(0, url.getParams().size());
    }

}
