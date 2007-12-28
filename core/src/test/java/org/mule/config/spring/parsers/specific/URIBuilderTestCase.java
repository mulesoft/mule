package org.mule.config.spring.parsers.specific;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

import java.util.HashMap;
import java.util.Map;

public class URIBuilderTestCase extends AbstractMuleTestCase
{

    private static final Map queries;

    static
    {
        queries = new HashMap();
        queries.put("aname", "avalue");
        queries.put("bname", "bvalue");
    }

    public void testAddressForProtocol()
    {
        URIBuilder uri = new URIBuilder();
        uri.setProtocol("foo");
        uri.setAddress("foo://bar");
        assertEquals("foo://bar", uri.toString());
    }

    public void testAddressForMeta()
    {
        URIBuilder uri = new URIBuilder();
        uri.setMeta("foo");
        uri.setAddress("baz://bar");
        assertEquals("foo:baz://bar", uri.toString());
    }

    public void testQueriesWithAddress()
    {
        URIBuilder uri = new URIBuilder();
        uri.setAddress("foo://bar");
        uri.setQueryMap(queries);
        assertEquals("foo://bar?aname=avalue&bname=bvalue", uri.toString());
    }

    // note that explicit properties over-rule those in the uri when duplicated
    // and we keep parameter ordering as in original url
    public void testLiteralQueries()
    {
        URIBuilder uri1 = new URIBuilder();
        uri1.setAddress("foo://bar?cname=cvalue&aname=anothervalue");
        uri1.setQueryMap(queries);
        assertEquals("foo://bar?cname=cvalue&aname=avalue&bname=bvalue", uri1.toString());
        URIBuilder uri2 = new URIBuilder();
        uri2.setQueryMap(queries);
        uri2.setAddress("foo://bar?cname=cvalue&aname=anothervalue");
        assertEquals("foo://bar?cname=cvalue&aname=avalue&bname=bvalue", uri2.toString());
    }

    public void testNullQueries()
    {
        URIBuilder uri1 = new URIBuilder();
        uri1.setAddress("foo://bar?cname&aname");
        uri1.setQueryMap(queries);
        assertEquals("foo://bar?cname&aname=avalue&bname=bvalue", uri1.toString());
    }

    public void testFromString()
    {
        URIBuilder uri = new URIBuilder("test://bar");
        UMOEndpointURI endpointURI = uri.getEndpoint();
        assertEquals("test://bar", endpointURI.getUri().toString());
        assertEquals("test", endpointURI.getSchemeMetaInfo());
        uri = new URIBuilder("meta:test://bar");
        endpointURI = uri.getEndpoint();
        assertEquals("test://bar", endpointURI.getUri().toString());
        assertEquals("meta", endpointURI.getSchemeMetaInfo());
    }

}