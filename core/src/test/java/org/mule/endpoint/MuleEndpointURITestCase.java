/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint;

import org.mule.api.MuleException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Properties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MuleEndpointURITestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testEquality() throws Exception
    {
        MuleEndpointURI u1 = new MuleEndpointURI("test://mule:secret@jabber.org:6666/ross@jabber.org", muleContext);
        MuleEndpointURI u2 = new MuleEndpointURI("test://mule:secret@jabber.org:6666/ross@jabber.org", muleContext);

        assertEquals(u1, u2);
        assertEquals(u2, u1);
        assertEquals(u1.hashCode(), u2.hashCode());
        assertEquals(u2.hashCode(), u1.hashCode());

        MuleEndpointURI u3 = new MuleEndpointURI(u1);
        assertEquals(u1, u3);
        assertEquals(u2, u3);
        assertEquals(u3, u1);
        assertEquals(u3, u2);
        assertEquals(u1.hashCode(), u3.hashCode());
        assertEquals(u2.hashCode(), u3.hashCode());
    }

    @Test
    public void testUriWithHostOnly() throws Exception
    {
        MuleEndpointURI uri = buildEndpointUri("test://theHost");
        assertSchemeAndHostAndEnpointName(uri);
        assertEquals(-1, uri.getPort());
        assertEquals(0, uri.getParams().size());
    }

    @Test
    public void testUriWithHostAndPort() throws Exception
    {
        MuleEndpointURI uri = buildEndpointUri("test://theHost:9999");
        assertSchemeAndHostAndEnpointName(uri);
        assertEquals(9999, uri.getPort());
        assertEquals(0, uri.getParams().size());
    }
    
    @Test
    public void testUriWithUsername() throws Exception
    {
        MuleEndpointURI uri = buildEndpointUri("test://theUser@theHost");
        assertSchemeAndHostAndEnpointName(uri);
        assertEquals(-1, uri.getPort());
        assertEquals("theUser", uri.getUser());
        assertEquals(0, uri.getParams().size());
    }
        
    @Test
    public void testUriWithUsernameAndPassword() throws Exception
    {
        MuleEndpointURI uri = buildEndpointUri("test://theUser:secret@theHost");
        assertSchemeAndHostAndEnpointName(uri);
        assertEquals(-1, uri.getPort());
        assertEquals("theUser", uri.getUser());
        assertEquals("secret", uri.getPassword());
        assertEquals("theUser:secret", uri.getUserInfo());
        assertEquals(0, uri.getParams().size());
    }
    
    @Test
    public void testUriWithUsernameContainingAtSignAndPassword() throws Exception
    {
        MuleEndpointURI uri = buildEndpointUri("test://theUser%40theEmailHost:secret@theHost");
        assertSchemeAndHostAndEnpointName(uri);
        assertEquals(-1, uri.getPort());
        assertEquals("theUser@theEmailHost", uri.getUser());
        assertEquals("secret", uri.getPassword());
        assertEquals(0, uri.getParams().size());
    }

    @Test
    public void testUriWithUsernameAndPasswordContainingAtSign() throws Exception
    {
        MuleEndpointURI uri = buildEndpointUri("test://theUser:secret%40secret@theHost");
        assertSchemeAndHostAndEnpointName(uri);
        assertEquals(-1, uri.getPort());
        assertEquals("theUser", uri.getUser());
        assertEquals("secret@secret", uri.getPassword());
        assertEquals(0, uri.getParams().size());
    }

    @Test
    public void testUriWithPath() throws Exception
    {
        MuleEndpointURI uri = buildEndpointUri("test://theHost/thePath");
        assertSchemeAndHostAndEnpointName(uri);
        assertEquals(-1, uri.getPort());
        assertEquals("/thePath", uri.getPath());
        assertEquals(0, uri.getParams().size());
    }
    
    @Test
    public void testUriWithQuery() throws Exception
    {
        MuleEndpointURI uri = buildEndpointUri("test://theHost?query=xxx");
        assertSchemeAndHostAndEnpointName(uri);
        assertEquals(-1, uri.getPort());
        assertEquals("query=xxx", uri.getQuery());
        
        Properties params = uri.getParams();
        assertEquals(1, params.size());
        assertEquals("xxx", params.getProperty("query"));
    }
    
    @Test
    public void testUriWithQueryContainingAtSign() throws Exception
    {
        MuleEndpointURI uri = buildEndpointUri("test://theHost?query=xxx@yyy");
        assertSchemeAndHostAndEnpointName(uri);
        assertEquals(-1, uri.getPort());
        assertEquals("query=xxx@yyy", uri.getQuery());
        
        Properties params = uri.getParams();
        assertEquals(1, params.size());
        assertEquals("xxx@yyy", params.getProperty("query"));
    }

    @Test
    public void testUriWithPathAndQuery() throws Exception
    {
        MuleEndpointURI uri = buildEndpointUri("test://theHost/thePath?query=xxx");
        assertSchemeAndHostAndEnpointName(uri);
        assertEquals(-1, uri.getPort());
        assertEquals("/thePath", uri.getPath());
        assertEquals("query=xxx", uri.getQuery());
        
        Properties params = uri.getParams();
        assertEquals(1, params.size());
        assertEquals("xxx", params.getProperty("query"));
    }
    
    @Test
    public void testPasswordMasking() throws Exception
    {
        MuleEndpointURI uri = new MuleEndpointURI("test://theUser:password@theHost", muleContext);
        assertEquals("test://theUser:****@theHost", uri.toString());
    }
    
    @Test
    public void testPasswordMaskingWithUsernameContainingAtSign() throws Exception
    {
        MuleEndpointURI uri = new MuleEndpointURI("test://theUser%40theEmailHost:password@theHost", muleContext);
        assertEquals("test://theUser%40theEmailHost:****@theHost", uri.toString());
    }
    
    private MuleEndpointURI buildEndpointUri(String uriString) throws MuleException
    {
        MuleEndpointURI uri = new MuleEndpointURI(uriString, muleContext);
        uri.initialise();
        return uri;
    }

    private void assertSchemeAndHostAndEnpointName(MuleEndpointURI uri)
    {
        assertEquals("test", uri.getScheme());
        assertEquals("theHost", uri.getHost());
        assertNull(uri.getEndpointName());
    }
}
