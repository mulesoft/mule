package org.mule.config.spring.parsers.specific;

import org.mule.tck.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.Map;

public class LazyEndpointURITestCase extends AbstractMuleTestCase
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
        LazyEndpointURI uri = new LazyEndpointURI();
        uri.setProtocol("foo");
        uri.setAddress("foo://bar");
        assertEquals("foo://bar", uri.toConstructor());
        assertEquals("foo://bar", uri.toString());
    }

    public void testAddressForMeta()
    {
        LazyEndpointURI uri = new LazyEndpointURI();
        uri.setMeta("foo");
        uri.setAddress("baz://bar");
        assertEquals("foo:baz://bar", uri.toConstructor());
    }

    public void testQueriesWithAddress()
    {
        LazyEndpointURI uri = new LazyEndpointURI();
        uri.setAddress("foo://bar");
        uri.setQueries(queries);
        assertEquals("foo://bar?aname=avalue&bname=bvalue", uri.toConstructor());
    }

    public void testLiteralQueries()
    {
        LazyEndpointURI uri = new LazyEndpointURI();
        uri.setAddress("foo://bar?cname=cvalue");
        uri.setQueries(queries);
        assertEquals("foo://bar?cname=cvalue&aname=avalue&bname=bvalue", uri.toConstructor());
    }

}
