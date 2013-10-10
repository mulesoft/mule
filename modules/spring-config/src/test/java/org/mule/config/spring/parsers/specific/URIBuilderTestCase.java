/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointURI;
import org.mule.endpoint.URIBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class URIBuilderTestCase extends AbstractMuleTestCase
{

    private static final Map queries;

    static
    {
        queries = new HashMap();
        queries.put("aname", "avalue");
        queries.put("bname", "bvalue");
    }

    private MuleContext unusedMuleContext = null;
    
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

    @Test
    public void testLiteralQueries()
    {
        URIBuilder uri = new URIBuilder(unusedMuleContext);
        uri.setAddress("foo://bar?cname=cvalue");
        uri.setQueryMap(queries);
        assertEquals("foo://bar?cname=cvalue&aname=avalue&bname=bvalue", uri.toString());
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

}
