/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.spring;

import org.mule.tck.FunctionalTestCase;

public class PropertyPlaceholderMule2150TestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/spring/property-placeholder-mule-2150-test.xml";
    }

    protected String getProperty(String name)
    {
        MapHolder holder = (MapHolder) managementContext.getRegistry().lookupObject("props");
        String value = (String) holder.getMap().get(name);
        assertNotNull(name, value);
        return value;
    }

    public void testMuleEnvironment()
    {
        assertEquals("value1", getProperty("prop1"));
    }

    public void testSpringPropertyPlaceholder()
    {
        assertEquals("value2", getProperty("prop2"));
    }

    public void testJavaEnvironment()
    {
        assertEquals(System.getProperty("java.version"), getProperty("prop3"));
    }

}
