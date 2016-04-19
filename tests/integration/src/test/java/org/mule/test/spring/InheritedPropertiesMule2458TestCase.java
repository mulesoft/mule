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

public class InheritedPropertiesMule2458TestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/spring/inherited-properties-mule-2458-test-flow.xml";
    }

    @Test
    public void testProperties()
    {
        Object flow = muleContext.getRegistry().lookupObject("service");
        assertNotNull(flow);
        ImmutableEndpoint endpoint = (ImmutableEndpoint) ((Flow)flow).getMessageSource();

        assertNotNull(endpoint);
        assertProperty(endpoint, "global-only", "global");
        assertProperty(endpoint, "local-only", "local");
        assertProperty(endpoint, "url-only", "url");

        assertProperty(endpoint, "global-and-local", "local");
        assertProperty(endpoint, "global-and-url", "global");
        assertProperty(endpoint, "local-and-url", "local");

        assertProperty(endpoint, "all", "local");
    }

    protected void assertProperty(ImmutableEndpoint endpoint, String key, String value)
    {
        Object property = endpoint.getProperty(key);
        assertNotNull("Property " + key + " is missing", property);
        String actual = property.toString();
        assertEquals("Unexpected value for " + key + ": " + actual + ", not " + value, value, actual);
    }
}
