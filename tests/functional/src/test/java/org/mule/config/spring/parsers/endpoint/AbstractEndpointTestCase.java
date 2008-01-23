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

import org.mule.RegistryContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.FunctionalTestCase;

public abstract class AbstractEndpointTestCase extends FunctionalTestCase
{

    public ImmutableEndpoint doTest(String name) throws MuleException
    {
        ImmutableEndpoint endpoint = RegistryContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(name);
        assertNotNull(endpoint);
        EndpointURI uri = endpoint.getEndpointURI();
        assertNotNull(uri);
        assertEquals("foo", uri.getAddress());
        assertEquals("test", uri.getScheme());
        return endpoint;
    }

}
