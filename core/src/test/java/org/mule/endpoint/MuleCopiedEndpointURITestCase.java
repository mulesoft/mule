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

import org.mule.api.endpoint.Endpoint;
import org.mule.api.endpoint.EndpointURI;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;

public class MuleCopiedEndpointURITestCase extends AbstractMuleTestCase
{

    /**
     * See MULE-2164
     * @throws Exception
     */
    public void testCopyMetaSchemeEndpointURI() throws Exception
    {

        // Create and test values
        Endpoint endpoint = MuleTestUtils.getTestSchemeMetaInfoEndpoint("testEndpoint",
            Endpoint.ENDPOINT_TYPE_SENDER, "protocol", muleContext);
        EndpointURI endpointUri = endpoint.getEndpointURI();
        assertEquals("protocol", endpointUri.getScheme());
        assertEquals("test", endpointUri.getSchemeMetaInfo());
        assertEquals("test:protocol", endpointUri.getFullScheme());
        assertEquals("test", endpointUri.getAddress());

        // Copy and test values
        EndpointURI newEndpointUri = new MuleEndpointURI(endpointUri);
        newEndpointUri.initialise();
        assertEquals("protocol", newEndpointUri.getScheme());
        assertEquals("test", newEndpointUri.getSchemeMetaInfo());
        assertEquals("test:protocol", newEndpointUri.getFullScheme());
        assertEquals("test", newEndpointUri.getAddress());
        assertEquals(endpointUri, newEndpointUri);
    }

}
