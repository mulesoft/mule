/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;

import org.junit.Test;

public class ComplexEndpointTestCase extends AbstractEndpointTestCase
{
    @Override
    protected String getConfigFile()
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
