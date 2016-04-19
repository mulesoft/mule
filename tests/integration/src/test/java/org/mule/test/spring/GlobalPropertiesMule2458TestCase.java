/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.construct.Flow;

import org.junit.Test;

public class GlobalPropertiesMule2458TestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/spring/global-properties-mule-2458-test-flow.xml";
    }

    @Test
    public void testProperties()
    {
        Object flow = muleContext.getRegistry().lookupObject("service");
        assertNotNull(flow);
        ImmutableEndpoint ep = (ImmutableEndpoint) ((Flow) flow).getMessageSource();

        assertNotNull(ep);
        assertEquals("local", ep.getProperties().get("local"));
        assertEquals("global", ep.getProperties().get("global"));
        assertEquals("local", ep.getProperties().get("override-me"));
        assertEquals(3, ep.getProperties().size());
    }
}
