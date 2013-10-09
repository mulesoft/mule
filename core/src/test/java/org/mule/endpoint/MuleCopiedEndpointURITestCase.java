/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint;

import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MuleCopiedEndpointURITestCase extends AbstractMuleContextTestCase
{

    /**
     * See MULE-2164
     * @throws Exception
     */
    @Test
    public void testCopyMetaSchemeEndpointURI() throws Exception
    {
        // Create and test values
        ImmutableEndpoint endpoint = MuleTestUtils.getTestSchemeMetaInfoOutboundEndpoint("testEndpoint", "protocol", muleContext);
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
