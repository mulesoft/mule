/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom;

import org.mule.api.service.Service;
import org.mule.module.atom.endpoint.AtomInboundEndpoint;
import org.mule.tck.FunctionalTestCase;

import java.text.SimpleDateFormat;

public class NamespaceTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "namespace-config.xml";
    }

    public void testEndpointConfig() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("test");
        assertNotNull(service);
        assertTrue(service.getInboundRouter().getEndpoints().get(0) instanceof AtomInboundEndpoint);
        AtomInboundEndpoint ep = (AtomInboundEndpoint) service.getInboundRouter().getEndpoints().get(0);
        assertNotNull(ep.getLastUpdate());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        assertEquals(sdf.parse("2009-10-01"), ep.getLastUpdate());
        assertEquals(1, ep.getAcceptedMimeTypes().size());
        assertTrue(ep.getAcceptedMimeTypes().contains("foo/bar"));
    }
}
