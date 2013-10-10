/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class PropertyPlaceholderDefaultTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/spring/property-placeholder-default-test.xml";
    }

    protected String getProperty(String name)
    {
        MapHolder holder = (MapHolder) muleContext.getRegistry().lookupObject("props");
        String value = (String) holder.getMap().get(name);
        assertNotNull(name, value);
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
