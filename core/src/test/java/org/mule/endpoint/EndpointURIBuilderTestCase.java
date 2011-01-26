/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint;

import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.EndpointURIBuilder;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.tck.AbstractMuleTestCase;

import java.net.URI;
import java.net.URISyntaxException;

public class EndpointURIBuilderTestCase extends AbstractMuleTestCase
{
    private static final String PLAIN_USERNAME_URI = "test://user:secret@theHost:42/path?key=value#fragment";
    private static final String EXPECTED_PLAIN_URI_STRING = "test://user:****@theHost:42/path?key=value#fragment";
    
    private static final String USERNAME_WITH_AT_SIGN_URI = "test://user%40host:secret@theHost:42/path?key=value#fragment";
    private static final String EXPECTED_AT_SIGN_URI_STRING = "test://user%40host:****@theHost:42/path?key=value#fragment";
    
    // Test for MULE-2720
    public void testGetPropertiesForURI() throws MalformedEndpointException, URISyntaxException
    {
        UrlEndpointURIBuilder endpointURIBuilder = new UrlEndpointURIBuilder();
        endpointURIBuilder.build(new URI("ftp://test%25user:test@192.168.1.12:21"), muleContext);
        assertEquals("test%user:test", endpointURIBuilder.userInfo);
    }
    
    public void testUrlEndpointBuilderPasswordMasking() throws Exception
    {
        UrlEndpointURIBuilder builder = new UrlEndpointURIBuilder();
        checkUriWithPlainUsername(builder);
    }
    
    public void testUrlEndpointBuilderPasswordMaskingWithAtSign() throws Exception
    {
        UrlEndpointURIBuilder builder = new UrlEndpointURIBuilder();
        checkUriWithUsernameContainingAtSign(builder);
    }
    
    public void testUserInfoEndpointBuilderPasswordMasking() throws Exception
    {
        UserInfoEndpointURIBuilder builder = new UserInfoEndpointURIBuilder();
        checkUriWithPlainUsername(builder);
    }

    public void testUserInfoEndpointBuilderPasswordMaskingWithAtSign() throws Exception
    {
        UserInfoEndpointURIBuilder builder = new UserInfoEndpointURIBuilder();
        checkUriWithUsernameContainingAtSign(builder);
    }
    
    private void checkUriWithPlainUsername(EndpointURIBuilder builder) throws Exception
    {
        URI inputUri = new URI(PLAIN_USERNAME_URI);
        EndpointURI uri = builder.build(inputUri, muleContext);
        
        assertEquals("user", uri.getUser());
        assertUriParts(uri);
        
        // assert that the password is properly masked
        assertEquals(EXPECTED_PLAIN_URI_STRING, uri.toString());
    }

    private void checkUriWithUsernameContainingAtSign(EndpointURIBuilder builder) throws Exception
    {
        URI inputUri = new URI(USERNAME_WITH_AT_SIGN_URI);
        EndpointURI uri = builder.build(inputUri, muleContext);
        
        assertEquals("user@host", uri.getUser());
        assertUriParts(uri);
        
        // assert that the password is properly masked
        assertEquals(EXPECTED_AT_SIGN_URI_STRING, uri.toString());
    }

    private void assertUriParts(EndpointURI uri)
    {
        // assert that the individual parts of the URI get preserved
        assertEquals("secret", uri.getPassword());
        assertEquals("theHost", uri.getHost());
        assertEquals(42, uri.getPort());
        assertEquals("/path", uri.getPath());
        assertEquals("key=value", uri.getQuery());
    }
}
