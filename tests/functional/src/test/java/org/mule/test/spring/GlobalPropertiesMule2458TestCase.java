/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.service.Service;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GlobalPropertiesMule2458TestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/spring/global-properties-mule-2458-test.xml";
    }

    @Test
    public void testProperties()
    {
        Service service = muleContext.getRegistry().lookupService("service");
        assertNotNull(service);
        ImmutableEndpoint ep = ((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoints().get(0);
        assertNotNull(ep);
        assertEquals("local", ep.getProperties().get("local"));
        assertEquals("global", ep.getProperties().get("global"));
        assertEquals("local", ep.getProperties().get("override-me"));
        assertEquals(3, ep.getProperties().size());
    }

}
