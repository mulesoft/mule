/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.endpoint;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;

public class MuleCopiedEndpointURITestCase extends AbstractMuleTestCase
{

    /**
     * See MULE-2164
     * @throws Exception
     */
    public void testCopyMetaSchemeEndpointURI() throws Exception
    {

        // Create and test values
        UMOEndpoint endpoint = MuleTestUtils.getTestSchemeMetaInfoEndpoint("testEndpoint",
            UMOEndpoint.ENDPOINT_TYPE_SENDER_AND_RECEIVER, "protocol", managementContext);
        UMOEndpointURI endpointUri = endpoint.getEndpointURI();
        assertEquals("protocol", endpointUri.getScheme());
        assertEquals("test", endpointUri.getSchemeMetaInfo());
        assertEquals("test:protocol", endpointUri.getFullScheme());
        assertEquals("test", endpointUri.getAddress());

        // Copy and test values
        UMOEndpointURI newEndpointUri = new MuleEndpointURI(endpointUri);
        newEndpointUri.initialise();
        assertEquals("protocol", newEndpointUri.getScheme());
        assertEquals("test", newEndpointUri.getSchemeMetaInfo());
        assertEquals("test:protocol", newEndpointUri.getFullScheme());
        assertEquals("test", newEndpointUri.getAddress());
        assertEquals(endpointUri, newEndpointUri);
    }

}
