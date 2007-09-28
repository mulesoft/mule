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

import org.mule.impl.MuleDescriptor;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

public class GlobalPropertiesMule2458TestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/spring/global-properties-mule-2458-test.xml";
    }

    public void testProperties()
    {
        MuleDescriptor descriptor = (MuleDescriptor)managementContext.getRegistry().lookupService("service");
        assertNotNull(descriptor);
        UMOImmutableEndpoint ep = (UMOImmutableEndpoint) descriptor.getInboundRouter().getEndpoints().get(0);
        assertNotNull(ep);
        assertEquals("local", ep.getProperties().get("local"));
        assertEquals("global", ep.getProperties().get("global"));
        assertEquals(2, ep.getProperties().size());
    }

}
