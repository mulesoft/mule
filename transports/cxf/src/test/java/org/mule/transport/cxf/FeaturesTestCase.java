/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.tck.FunctionalTestCase;
import org.mule.api.endpoint.ImmutableEndpoint;

import java.util.Map;
import java.util.List;

import org.apache.cxf.feature.LoggingFeature;

public class FeaturesTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "features-test.xml";
    }

    public void testFeatures() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointBuilder("endpoint").buildInboundEndpoint();
        assertNotNull(endpoint);
        Map properties = endpoint.getProperties();
        assertNotNull(properties);
        assertEquals(5, properties.size());
        assertNotNull(properties.get("features"));
        assertTrue(properties.get("features") instanceof List);
        List features = (List) properties.get("features");
        assertEquals(2, features.size());
        assertTrue(features.get(0) instanceof LoggingFeature);
    }

}
