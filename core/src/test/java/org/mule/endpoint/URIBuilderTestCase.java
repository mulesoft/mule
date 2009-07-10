/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint;

import org.mule.api.endpoint.EndpointURI;
import org.mule.tck.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.Map;

public class URIBuilderTestCase extends AbstractMuleTestCase
{

    private static final Map<String, String> queries;

    static
    {
        queries = new HashMap<String, String>();
        queries.put("aname", "avalue");
        queries.put("bname", "bvalue");
    }

    public void testAddressForProtocol()
    {
        URIBuilder uri = new URIBuilder(muleContext);
        uri.setProtocol("foo");
        uri.setAddress("foo://bar");
        assertEquals("foo://bar", uri.toString());
    }

    public void testAddressForMeta()
    {
        URIBuilder uri = new URIBuilder(muleContext);
        uri.setMeta("foo");
        uri.setAddress("baz://bar");
        assertEquals("foo:baz://bar", uri.toString());
    }

    public void testQueriesWithAddress()
    {
        URIBuilder uri = new URIBuilder(muleContext);
        uri.setAddress("foo://bar");
        uri.setQueryMap(queries);
        assertEquals("foo://bar?aname=avalue&bname=bvalue", uri.toString());
    }

    // note that explicit properties over-rule those in the uri when duplicated
    // and we keep parameter ordering as in original url
    public void testLiteralQueries()
    {
        URIBuilder uri1 = new URIBuilder(muleContext);
        uri1.setAddress("foo://bar?cname=cvalue&aname=anothervalue");
        uri1.setQueryMap(queries);
        assertEquals("foo://bar?cname=cvalue&aname=avalue&bname=bvalue", uri1.toString());
        URIBuilder uri2 = new URIBuilder(muleContext);
        uri2.setQueryMap(queries);
        uri2.setAddress("foo://bar?cname=cvalue&aname=anothervalue");
        assertEquals("foo://bar?cname=cvalue&aname=avalue&bname=bvalue", uri2.toString());
    }

    public void testNullQueries()
    {
        URIBuilder uri1 = new URIBuilder(muleContext);
        uri1.setAddress("foo://bar?cname&aname");
        uri1.setQueryMap(queries);
        assertEquals("foo://bar?cname&aname=avalue&bname=bvalue", uri1.toString());
    }

    public void testFromString()
    {
        URIBuilder uri = new URIBuilder("test://bar", muleContext);
        EndpointURI endpointURI = uri.getEndpoint();
        assertEquals("test://bar", endpointURI.getUri().toString());
        assertEquals("test", endpointURI.getSchemeMetaInfo());
        uri = new URIBuilder("meta:test://bar", muleContext);
        endpointURI = uri.getEndpoint();
        assertEquals("test://bar", endpointURI.getUri().toString());
        assertEquals("meta", endpointURI.getSchemeMetaInfo());
    }

    public void testMultiValueParam() 
    {
        // Test from uri string
        URIBuilder uri = new URIBuilder("test://bar?aname=avalue&aname=bvalue&aname=cvalue", muleContext);
        EndpointURI endpointURI = uri.getEndpoint();
        assertEquals("test://bar?aname=avalue&aname=bvalue&aname=cvalue", endpointURI.getUri().toString());

        // Test modifying values with a map (only first should change)
        Map<String, String> tq = new HashMap<String, String>();
        tq.put("aname", "zvalue");
        tq.put("dname", "dvalue");

        uri = new URIBuilder("test://bar?aname=avalue&aname=bvalue&aname=cvalue", muleContext);
        uri.setQueryMap(tq);
        endpointURI = uri.getEndpoint();
        assertEquals("test://bar?aname=zvalue&aname=bvalue&aname=cvalue&dname=dvalue", endpointURI.getUri().toString());
    }
    
}