/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.EndpointURIBuilder;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

@SmallTest
public class EndpointURIBuilderTestCase extends AbstractMuleTestCase
{
    private static final String PLAIN_USERNAME_URI = "test://user:secret@theHost:42/path?key=value#fragment";
    private static final String EXPECTED_PLAIN_URI_STRING = "test://user:****@theHost:42/path?key=value#fragment";
    
    private static final String USERNAME_WITH_AT_SIGN_URI = "test://user%40host:secret@theHost:42/path?key=value#fragment";
    private static final String EXPECTED_AT_SIGN_URI_STRING = "test://user%40host:****@theHost:42/path?key=value#fragment";
    private MuleContext unusedMuleContext = null;
    
    // Test for MULE-2720
    @Test
    public void testGetPropertiesForURI() throws MalformedEndpointException, URISyntaxException
    {
        UrlEndpointURIBuilder endpointURIBuilder = new UrlEndpointURIBuilder();
        endpointURIBuilder.build(new URI("ftp://test%25user:test@192.168.1.12:21"), unusedMuleContext);
        assertEquals("test%user:test", endpointURIBuilder.userInfo);
    }
    
    @Test
    public void testUrlEndpointBuilderPasswordMasking() throws Exception
    {
        UrlEndpointURIBuilder builder = new UrlEndpointURIBuilder();
        checkUriWithPlainUsername(builder);
    }
    
    @Test
    public void testUrlEndpointBuilderPasswordMaskingWithAtSign() throws Exception
    {
        UrlEndpointURIBuilder builder = new UrlEndpointURIBuilder();
        checkUriWithUsernameContainingAtSign(builder);
    }
    
    @Test
    public void testUserInfoEndpointBuilderPasswordMasking() throws Exception
    {
        UserInfoEndpointURIBuilder builder = new UserInfoEndpointURIBuilder();
        checkUriWithPlainUsername(builder);
    }

    @Test
    public void testUserInfoEndpointBuilderPasswordMaskingWithAtSign() throws Exception
    {
        UserInfoEndpointURIBuilder builder = new UserInfoEndpointURIBuilder();
        checkUriWithUsernameContainingAtSign(builder);
    }

    @Test
    public void testQueryParameterWithEncodedAmpersand() throws Exception
    {
        UrlEndpointURIBuilder endpointURIBuilder = new UrlEndpointURIBuilder();
        EndpointURI endpointURI = endpointURIBuilder.build(new URI("http://host/path?key=value%26test"), unusedMuleContext);

        assertThat(endpointURI.getParams().size(), is(1));
        assertThat(endpointURI.getParams().getProperty("key"), equalTo("value&test"));
    }

    private void checkUriWithPlainUsername(EndpointURIBuilder builder) throws Exception
    {
        URI inputUri = new URI(PLAIN_USERNAME_URI);
        EndpointURI uri = builder.build(inputUri, unusedMuleContext);
        
        assertEquals("user", uri.getUser());
        assertUriParts(uri);
        
        // assert that the password is properly masked
        assertEquals(EXPECTED_PLAIN_URI_STRING, uri.toString());
    }

    private void checkUriWithUsernameContainingAtSign(EndpointURIBuilder builder) throws Exception
    {
        URI inputUri = new URI(USERNAME_WITH_AT_SIGN_URI);
        EndpointURI uri = builder.build(inputUri, unusedMuleContext);
        
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
