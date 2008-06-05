/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.tck.FunctionalTestCase;

import java.util.Properties;

public class StartupPropertiesTestCase extends FunctionalTestCase
{
    
    private String STARTUP_PROPERTY_1_KEY = "startupProperty1";
    private String STARTUP_PROPERTY_2_KEY = "startupProperty2";
    private String STARTUP_PROPERTY_1_VALUE = "startupProperty1Value";
    private String STARTUP_PROPERTY_2_VALUE = "startupProperty2Value";
    
    
    protected String getConfigResources()
    {
        return "org/mule/test/config/startup-properties-test.xml";
    }
    
    protected Properties getStartUpProperties()
    {
        Properties p = new Properties();
        p.setProperty(STARTUP_PROPERTY_1_KEY, STARTUP_PROPERTY_1_VALUE);
        p.setProperty(STARTUP_PROPERTY_2_KEY, STARTUP_PROPERTY_2_VALUE);
        return p;
    }
    
    public void testStartProperties()
    {
        Object property1 = muleContext.getRegistry().lookupObject(STARTUP_PROPERTY_1_KEY);
        Object property2 = muleContext.getRegistry().lookupObject(STARTUP_PROPERTY_2_KEY);
        assertNotNull(property1);
        assertNotNull(property2);
        assertEquals(STARTUP_PROPERTY_1_VALUE, property1);
        assertEquals(STARTUP_PROPERTY_2_VALUE, property2);
    }

}
