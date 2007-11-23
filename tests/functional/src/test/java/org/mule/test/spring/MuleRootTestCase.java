/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.spring;

import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

public class MuleRootTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/spring/mule-root-test.xml";
    }

    public void testModel() throws UMOException
    {
        assertNotNull("No model", managementContext.getRegistry().lookupModel("model"));
        UMOImmutableEndpoint endpoint = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("endpoint");
        assertNotNull("No endpoint", endpoint);
        String address = endpoint.getEndpointURI().getAddress();
        assertNotNull("No address", address);
        assertEquals("value", address);
    }

}
