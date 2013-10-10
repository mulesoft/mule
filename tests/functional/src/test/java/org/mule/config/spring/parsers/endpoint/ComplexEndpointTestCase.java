/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.endpoint;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ComplexEndpointTestCase extends AbstractEndpointTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/endpoint/complex-endpoint-test.xml";
    }

    @Test
    public void testComplex() throws MuleException
    {
        ImmutableEndpoint endpoint = doTest("complex");
        assertNotNull(endpoint.getProperty("foo"));
        assertEquals(endpoint.getProperty("foo"), "123");
        assertNotNull(endpoint.getProperty("string"));
        assertEquals(endpoint.getProperty("string"), "hello world");
    }

}
