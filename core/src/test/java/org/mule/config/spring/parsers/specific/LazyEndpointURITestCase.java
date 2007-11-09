package org.mule.config.spring.parsers.specific;

import org.mule.tck.AbstractMuleTestCase;

public class LazyEndpointURITestCase extends AbstractMuleTestCase
{

    public void testAddressForProtocol()
    {
        LazyEndpointURI uri = new LazyEndpointURI();
        uri.setProtocol("foo");
        uri.setAddress("foo://bar");
        assertEquals("foo://bar", uri.toString());
    }

    public void testAddressForMeta()
    {
        LazyEndpointURI uri = new LazyEndpointURI();
        uri.setMeta("foo");
        uri.setAddress("baz://bar");
        assertEquals("foo:baz://bar", uri.toString());
    }

}
