/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.endpoint;

import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.junit4.FunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractEndpointTestCase extends FunctionalTestCase
{

    public ImmutableEndpoint doTest(String name) throws MuleException
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(name);
        assertNotNull(endpoint);
        EndpointURI uri = endpoint.getEndpointURI();
        assertNotNull(uri);
        assertEquals("foo", uri.getAddress());
        assertEquals("test", uri.getScheme());
        return endpoint;
    }

}
