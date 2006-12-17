/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.providers.email;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.AbstractMuleTestCase;

public class EmailEndpointsTestCase extends AbstractMuleTestCase
{

    public void testPop3Url() throws Exception
    {
        MuleEndpointURI endpointUri = new MuleEndpointURI("pop3://username:password@pop3.lotsofmail.org");
        assertEquals("pop3", endpointUri.getScheme());
        assertEquals("username@lotsofmail.org", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(-1, endpointUri.getPort());
        assertEquals("pop3.lotsofmail.org", endpointUri.getHost());
        assertEquals("username:password", endpointUri.getUserInfo());
        assertEquals("pop3://username:password@pop3.lotsofmail.org", endpointUri.toString());
        assertEquals(0, endpointUri.getParams().size());
    }

    public void testSmtpUrl() throws Exception
    {
        MuleEndpointURI url = new MuleEndpointURI("smtp://username:password@smtp.lotsofmail.org");
        assertEquals("smtp", url.getScheme());
        assertEquals("username@lotsofmail.org", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(-1, url.getPort());
        assertEquals("smtp.lotsofmail.org", url.getHost());
        assertEquals("username:password", url.getUserInfo());
        assertEquals("smtp://username:password@smtp.lotsofmail.org", url.toString());
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

    public void testImapUrlWithFolder() throws Exception
    {
        MuleEndpointURI endpointUri = new MuleEndpointURI(
            "imap://username:password@imap.lotsofmail.org/MyMail");
        assertEquals("imap", endpointUri.getScheme());
        assertEquals("username@lotsofmail.org", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(-1, endpointUri.getPort());
        assertEquals("imap.lotsofmail.org", endpointUri.getHost());
        assertEquals("username:password", endpointUri.getUserInfo());
        assertEquals("imap://username:password@imap.lotsofmail.org/MyMail", endpointUri.toString());
        assertEquals(0, endpointUri.getParams().size());
        assertEquals("/MyMail", endpointUri.getPath());

    }

    public void testSmtpUrlEmailUsernameAndParams() throws Exception
    {
        MuleEndpointURI endpointUri = new MuleEndpointURI(
            "smtp://test%40lotsofmail.org:password@smtpout.secureserver.net:3535?address=test@lotsofmail.org&ccAddresses=donkey@lotsofmail.org");
        assertEquals("smtp", endpointUri.getScheme());
        assertEquals("test@lotsofmail.org", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(3535, endpointUri.getPort());
        assertEquals("smtpout.secureserver.net", endpointUri.getHost());
        assertEquals("test@lotsofmail.org:password", endpointUri.getUserInfo());
        assertEquals(
            "smtp://test%40lotsofmail.org:password@smtpout.secureserver.net:3535?address=test@lotsofmail.org&ccAddresses=donkey@lotsofmail.org",
            endpointUri.toString());
        assertEquals(2, endpointUri.getParams().size());
        assertEquals("donkey@lotsofmail.org", endpointUri.getParams().get("ccAddresses"));

    }

    /**
     * Mule will assume that the username is the from address as it has an @ symbol
     * in it
     * @throws Exception
     */
    public void testSmtpUrlEmailUsernameWithoutAddressParam() throws Exception
    {
        MuleEndpointURI endpointUri = new MuleEndpointURI(
            "smtp://test%40lotsofmail.org:password@smtpout.secureserver.net:3535?ccAddresses=donkey@lotsofmail.org");
        assertEquals("smtp", endpointUri.getScheme());
        assertEquals("test@lotsofmail.org", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(3535, endpointUri.getPort());
        assertEquals("smtpout.secureserver.net", endpointUri.getHost());
        assertEquals("test@lotsofmail.org:password", endpointUri.getUserInfo());
        assertEquals(
            "smtp://test%40lotsofmail.org:password@smtpout.secureserver.net:3535?ccAddresses=donkey@lotsofmail.org",
            endpointUri.toString());
        assertEquals(1, endpointUri.getParams().size());
        assertEquals("donkey@lotsofmail.org", endpointUri.getParams().get("ccAddresses"));

    }

    /**
     * Mule will assume that the username is the from address as it has an @ symbol
     * in it
     * @throws Exception
     */
    public void testSmtpWithoutCredentials() throws Exception
    {
        MuleEndpointURI endpointUri = new MuleEndpointURI(
            "smtp://smtpout.secureserver.net:3535?address=test@lotsofmail.org");
        assertEquals("smtp", endpointUri.getScheme());
        assertEquals("test@lotsofmail.org", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(3535, endpointUri.getPort());
        assertEquals("smtpout.secureserver.net", endpointUri.getHost());
        assertNull(endpointUri.getUserInfo());
        assertEquals("smtp://smtpout.secureserver.net:3535?address=test@lotsofmail.org",
            endpointUri.toString());
        assertEquals(1, endpointUri.getParams().size());
    }

    /**
     * Added by Lajos on 2006-12-14 per Ross
     */
    public void testWithAddressOverrideOnly() throws Exception
    {
        MuleEndpointURI endpointUri = new MuleEndpointURI("smtp://?address=test@lotsofmail.org");
        assertEquals("smtp", endpointUri.getScheme());
        assertEquals("test@lotsofmail.org", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(-1, endpointUri.getPort());
        assertNull(endpointUri.getHost());
        assertNull(endpointUri.getUserInfo());
        assertEquals("smtp://?address=test@lotsofmail.org", endpointUri.toString());
        assertEquals(1, endpointUri.getParams().size());
    }

}
