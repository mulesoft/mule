/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.endpoint;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.endpoint.EndpointURI;
import org.mule.runtime.core.endpoint.URIBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

@SmallTest
public class URIBuilderTestCase extends AbstractMuleTestCase
{

    private static final String PWD_WITH_SPECIAL_CHARS = "! \"#$%&'()*+,-./:;<=>?@[\\]_`{|}~";
    private static final Map<String, String> queries;

    static
    {
        queries = new HashMap<String, String>();
        queries.put("aname", "avalue");
        queries.put("bname", "bvalue");
    }

    protected MuleContext unusedMuleContext = null;

    @Test
    public void testAddressForProtocol()
    {
        URIBuilder uri = new URIBuilder(unusedMuleContext);
        uri.setProtocol("foo");
        uri.setAddress("foo://bar");
        assertEquals("foo://bar", uri.toString());
    }

    @Test
    public void testAddressForMeta()
    {
        URIBuilder uri = new URIBuilder(unusedMuleContext);
        uri.setMeta("foo");
        uri.setAddress("baz://bar");
        assertEquals("foo:baz://bar", uri.toString());
    }

    @Test
    public void testQueriesWithAddress()
    {
        URIBuilder uri = new URIBuilder(unusedMuleContext);
        uri.setAddress("foo://bar");
        uri.setQueryMap(queries);
        assertEquals("foo://bar?aname=avalue&bname=bvalue", uri.toString());
    }

    // note that explicit properties over-rule those in the uri when duplicated
    // and we keep parameter ordering as in original url
    @Test
    public void testLiteralQueries()
    {
        URIBuilder uri1 = new URIBuilder(unusedMuleContext);
        uri1.setAddress("foo://bar?cname=cvalue&aname=anothervalue");
        uri1.setQueryMap(queries);
        assertEquals("foo://bar?cname=cvalue&aname=avalue&bname=bvalue", uri1.toString());
        URIBuilder uri2 = new URIBuilder(unusedMuleContext);
        uri2.setQueryMap(queries);
        uri2.setAddress("foo://bar?cname=cvalue&aname=anothervalue");
        assertEquals("foo://bar?cname=cvalue&aname=avalue&bname=bvalue", uri2.toString());
    }

    @Test
    public void testNullQueries()
    {
        URIBuilder uri1 = new URIBuilder(unusedMuleContext);
        uri1.setAddress("foo://bar?cname&aname");
        uri1.setQueryMap(queries);
        assertEquals("foo://bar?cname&aname=avalue&bname=bvalue", uri1.toString());
    }

    @Test
    public void testFromString()
    {
        URIBuilder uri = new URIBuilder("test://bar", unusedMuleContext);
        EndpointURI endpointURI = uri.getEndpoint();
        assertEquals("test://bar", endpointURI.getUri().toString());
        assertEquals("test", endpointURI.getSchemeMetaInfo());
        uri = new URIBuilder("meta:test://bar", unusedMuleContext);
        endpointURI = uri.getEndpoint();
        assertEquals("test://bar", endpointURI.getUri().toString());
        assertEquals("meta", endpointURI.getSchemeMetaInfo());
    }

    @Test
    public void testMultiValueParam() 
    {
        // Test from uri string
        URIBuilder uri = new URIBuilder("test://bar?aname=avalue&aname=bvalue&aname=cvalue", unusedMuleContext);
        EndpointURI endpointURI = uri.getEndpoint();
        assertEquals("test://bar?aname=avalue&aname=bvalue&aname=cvalue", endpointURI.getUri().toString());

        // Test modifying values with a map (only first should change)
        Map<String, String> tq = new HashMap<String, String>();
        tq.put("aname", "zvalue");
        tq.put("dname", "dvalue");

        uri = new URIBuilder("test://bar?aname=avalue&aname=bvalue&aname=cvalue", unusedMuleContext);
        uri.setQueryMap(tq);
        endpointURI = uri.getEndpoint();
        assertEquals("test://bar?aname=zvalue&aname=bvalue&aname=cvalue&dname=dvalue", endpointURI.getUri().toString());
    }

    @Test
    public void testConstructAddress()
    {
        URIBuilder uri = createURIBuilder("localhost", 8080, "http", "/test");
        String result = uri.getEncodedConstructor();
        assertEquals("http://localhost:8080/test", result);
    }

    @Test
    public void testConstructAddressWithRootTrailingSlashInPath()
    {
        URIBuilder uri = createURIBuilder("localhost", 8080, "http", "/");
        String result = uri.getEncodedConstructor();
        assertEquals("http://localhost:8080", result);

    }

    @Test
    public void testConstructAddressWithRootTrailingSlashInAddress()
    {
        URIBuilder uri = new URIBuilder();
        uri.setAddress("http://localhost:8080/");
        String result = uri.getEncodedConstructor();
        assertEquals("http://localhost:8080", result);
    }

    @Test
    public void testConstructAddressWithTrailingSlashInPath()
    {
        URIBuilder uri = createURIBuilder("localhost", 8080, "http", "/test/");
        String result = uri.getEncodedConstructor();
        assertEquals("http://localhost:8080/test/", result);
    }

    @Test
    public void testConstructAddressWithTrailingSlashInAddress()
    {
        URIBuilder uri = new URIBuilder();
        uri.setAddress("http://localhost:8080/test/");
        String result = uri.getEncodedConstructor();
        assertEquals("http://localhost:8080/test/", result);
    }

    @Test
    public void testConstructAddressWithParamsInRootPath() {
        String address = "http://localhost:8080/?key1=value1";
        URIBuilder uri = new URIBuilder();
        uri.setAddress(address);
        String result = uri.getEncodedConstructor();
        assertEquals(address, result);
    }

    /**
     * MULE-6279
     * @throws UnsupportedEncodingException 
     */
    @Test
    public void testSetAddressWithSpecialURIChars() throws UnsupportedEncodingException
    {
        String address = String.format("smtp://user%%40my-domain.com:%s@smtp.my-domain.com:25", encode(PWD_WITH_SPECIAL_CHARS, UTF_8.name()));
        URIBuilder uri = new URIBuilder();
        uri.setAddress(address);
        String result = uri.getEncodedConstructor();
        assertThat(result, is(address));
    }

    /**
     * MULE-6139
     * @throws UnsupportedEncodingException 
     */
    @Test
    public void testConstructAddressWithSpecialURIChars() throws UnsupportedEncodingException
    {
        String address = String.format("smtp://user%%40my-domain.com:%s@smtp.my-domain.com:25", encode(PWD_WITH_SPECIAL_CHARS, UTF_8.name()));
        URIBuilder uri = new URIBuilder();
        uri.setProtocol("smtp");
        uri.setUser("user@my-domain.com");
        uri.setPassword(PWD_WITH_SPECIAL_CHARS);
        uri.setHost("smtp.my-domain.com");
        uri.setPort("25");
        String result = uri.getEncodedConstructor();
        assertThat(result, is(address));
    }

    @Test
    public void testConstructAddressWithSpecialURICharsInPath() throws UnsupportedEncodingException
    {
        URIBuilder uri = new URIBuilder();
        uri.setProtocol("http");
        uri.setHost("my-domain.com");
        uri.setPath("folder%a/resource%b");
        String result = uri.getEncodedConstructor();
        assertThat(result, is("http://my-domain.com/folder%a/resource%b"));
    }

    private URIBuilder createURIBuilder(String host, int port, String protocol, String path)
    {
        URIBuilder builder = new URIBuilder();
        builder.setHost(host);
        builder.setPort(port);
        builder.setProtocol(protocol);
        builder.setPath(path);
        return builder;
    }
}
