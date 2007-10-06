/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.endpoint;

import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

public class ComplexEndpointTestCase extends AbstractEndpointTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/endpoint/complex-endpoint-test.xml";
    }

    public void testComplex() throws UMOException
    {
        UMOImmutableEndpoint endpoint = doTest("complex");
        assertNotNull(endpoint.getProperty("foo"));
        assertEquals(endpoint.getProperty("foo"), "123");
        assertNotNull(endpoint.getProperty("string"));
        assertEquals(endpoint.getProperty("string"), "hello world");
    }

}
