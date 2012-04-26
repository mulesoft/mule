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

import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointURI;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

@SmallTest
public class URIBuilderTestCase extends AbstractMuleTestCase
{

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
        URIBuilder uri = new URIBuilder();
        uri.setHost("localhost");
        uri.setPort(8080);
        uri.setProtocol("http");
        uri.setPath("/test");

        String result = uri.getEncodedConstructor();
        assertEquals("http://localhost:8080/test", result);
    }


}
