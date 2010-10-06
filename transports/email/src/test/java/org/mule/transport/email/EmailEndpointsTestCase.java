/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.email;

import org.mule.api.MuleException;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.AbstractMuleTestCase;

public class EmailEndpointsTestCase extends AbstractMuleTestCase
{
    private static final int PORT = 3125;
    
    public void testPop3Url() throws Exception
    {
        MuleEndpointURI endpointUri = buildEndpointUri("pop3://username:password@pop3.lotsofmail.org");
        
        assertEquals("pop3", endpointUri.getScheme());
        assertEquals("username@lotsofmail.org", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(-1, endpointUri.getPort());
        assertEquals("pop3.lotsofmail.org", endpointUri.getHost());
        assertEquals("username:password", endpointUri.getUserInfo());
        assertEquals("pop3://username:****@pop3.lotsofmail.org", endpointUri.toString());
        assertEquals(0, endpointUri.getParams().size());
    }

    public void testSmtpUrl() throws Exception
    {
        MuleEndpointURI endpointUri = buildEndpointUri("smtp://username:password@smtp.lotsofmail.org");
        
        assertEquals("smtp", endpointUri.getScheme());
        assertEquals("username@lotsofmail.org", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(-1, endpointUri.getPort());
        assertEquals("smtp.lotsofmail.org", endpointUri.getHost());
        assertEquals("username:password", endpointUri.getUserInfo());
        assertEquals("smtp://username:****@smtp.lotsofmail.org", endpointUri.toString());
        assertEquals(0, endpointUri.getParams().size());
    }

    public void testSmtpUrlWithPort() throws Exception
    {
        MuleEndpointURI endpointUri = buildEndpointUri("smtp://user:password@hostname:%1d", PORT);
            
        assertEquals("smtp", endpointUri.getScheme());
        assertEquals("user@hostname:" + PORT, endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(PORT, endpointUri.getPort());
        assertEquals("hostname", endpointUri.getHost());
        assertEquals("user:password", endpointUri.getUserInfo());
        assertEquals("smtp://user:****@hostname:" + PORT, endpointUri.toString());
        assertEquals(0, endpointUri.getParams().size());
    }

    public void testImapUrlWithFolder() throws Exception
    {
        MuleEndpointURI endpointUri = buildEndpointUri("imap://username:password@imap.lotsofmail.org/MyMail");
        
        assertEquals(ImapConnector.IMAP, endpointUri.getScheme());
        assertEquals("username@lotsofmail.org", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(-1, endpointUri.getPort());
        assertEquals("imap.lotsofmail.org", endpointUri.getHost());
        assertEquals("username:password", endpointUri.getUserInfo());
        assertEquals("imap://username:****@imap.lotsofmail.org/MyMail", endpointUri.toString());
        assertEquals(0, endpointUri.getParams().size());
        assertEquals("/MyMail", endpointUri.getPath());
    }

    public void testSmtpUrlEmailUsernameAndParams() throws Exception
    {
        String uri = "smtp://test%%40lotsofmail.org:password@smtpout.secureserver.net:%1d?address=test@lotsofmail.org&ccAddresses=donkey@lotsofmail.org";
        MuleEndpointURI endpointUri = buildEndpointUri(uri, PORT);
        
        assertEquals("smtp", endpointUri.getScheme());
        assertEquals("test@lotsofmail.org", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(PORT, endpointUri.getPort());
        assertEquals("smtpout.secureserver.net", endpointUri.getHost());
        assertEquals("test@lotsofmail.org:password", endpointUri.getUserInfo());
        assertEquals(
            "smtp://test%40lotsofmail.org:password@smtpout.secureserver.net:" + PORT +
            "?address=test@lotsofmail.org&ccAddresses=donkey@lotsofmail.org",
            endpointUri.toString());
        assertEquals(2, endpointUri.getParams().size());
        assertEquals("donkey@lotsofmail.org", endpointUri.getParams().get("ccAddresses"));
    }

    /**
     * Mule will assume that the username is the from address as it has an @ symbol
     * in it
     */
    public void testSmtpUrlEmailUsernameWithoutAddressParam() throws Exception
    {
        String uri = "smtp://test%%40lotsofmail.org:password@smtpout.secureserver.net:%1d?ccAddresses=donkey@lotsofmail.org";
        MuleEndpointURI endpointUri = buildEndpointUri(uri, PORT);
        
        assertEquals("smtp", endpointUri.getScheme());
        assertEquals("test@lotsofmail.org", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(PORT, endpointUri.getPort());
        assertEquals("smtpout.secureserver.net", endpointUri.getHost());
        assertEquals("test@lotsofmail.org:password", endpointUri.getUserInfo());
        assertEquals(
            "smtp://test%40lotsofmail.org:password@smtpout.secureserver.net:" + PORT +
            "?ccAddresses=donkey@lotsofmail.org",
            endpointUri.toString());
        assertEquals(1, endpointUri.getParams().size());
        assertEquals("donkey@lotsofmail.org", endpointUri.getParams().get("ccAddresses"));
    }

    /**
     * Mule will assume that the username is the from address as it has an @ symbol
     * in it
     */
    public void testSmtpWithoutCredentials() throws Exception
    {
        String uri = "smtp://smtpout.secureserver.net:%1d?address=test@lotsofmail.org";
        MuleEndpointURI endpointUri = buildEndpointUri(uri, PORT);
        
        assertEquals("smtp", endpointUri.getScheme());
        assertEquals("test@lotsofmail.org", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(PORT, endpointUri.getPort());
        assertEquals("smtpout.secureserver.net", endpointUri.getHost());
        assertNull(endpointUri.getUserInfo());
        assertEquals("smtp://smtpout.secureserver.net:" + PORT + "?address=test@lotsofmail.org",
            endpointUri.toString());
        assertEquals(1, endpointUri.getParams().size());
    }

    /**
     * Added by Lajos on 2006-12-14 per Ross
     */
    public void testWithAddressOverrideOnly() throws Exception
    {
        MuleEndpointURI endpointUri = buildEndpointUri("smtp://?address=test@lotsofmail.org");
        
        assertEquals("smtp", endpointUri.getScheme());
        assertEquals("test@lotsofmail.org", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(-1, endpointUri.getPort());
        assertNull(endpointUri.getHost());
        assertNull(endpointUri.getUserInfo());
        assertEquals("smtp://?address=test@lotsofmail.org", endpointUri.toString());
        assertEquals(1, endpointUri.getParams().size());
    }

    private MuleEndpointURI buildEndpointUri(String uriFormat, Object... args) throws MuleException
    {
        String uri = String.format(uriFormat, args);
        return buildEndpointUri(uri);
    }

    private MuleEndpointURI buildEndpointUri(String uri) throws MuleException
    {
        MuleEndpointURI endpointUri = new MuleEndpointURI(uri, muleContext);
        endpointUri.initialise();
        return endpointUri;
    }
}
