/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class PropertyPlaceholderDefaultTestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/spring/property-placeholder-default-test.xml";
    }

    protected String getProperty(String propertyName)
    {
        MapHolder holder = (MapHolder) muleContext.getRegistry().lookupObject("props");
        String value = (String) holder.getMap().get(propertyName);
        assertNotNull(propertyName, value);
        return value;
    }

    @Test
    public void testSpringPropertyNotDefinedAndDefault()
    {
        assertEquals("default1", getProperty("prop1"));
    }

    @Test
    public void testSpringPropertyDefinedAndDefault()
    {
        assertEquals("value2", getProperty("prop2"));
    }

}
