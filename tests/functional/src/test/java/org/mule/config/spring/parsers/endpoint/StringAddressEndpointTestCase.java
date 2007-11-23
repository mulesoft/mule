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

import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

public class StringAddressEndpointTestCase extends AbstractEndpointTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/endpoint/string-address-endpoint-test.xml";
    }

    public void testStringAddress() throws UMOException
    {
        doTest("string");
    }

    public void testOrphanAddress() throws UMOException
    {
        doTest("orphan");
    }

    public void testChildAddress() throws UMOException
    {
        UMOComponent component = managementContext.getRegistry().lookupComponent("service");
        UMOImmutableEndpoint endpoint = (UMOImmutableEndpoint) component.getInboundRouter().getEndpoints().get(0);
        assertNotNull(endpoint);
        UMOEndpointURI uri = endpoint.getEndpointURI();
        assertNotNull(uri);
        assertEquals("foo", uri.getAddress());
        assertEquals("test", uri.getScheme());
    }

}
