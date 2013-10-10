/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.config;

import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Properties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class StartupPropertiesTestCase extends FunctionalTestCase
{
    private String STARTUP_PROPERTY_1_KEY = "startupProperty1";
    private String STARTUP_PROPERTY_2_KEY = "startupProperty2";
    private String STARTUP_PROPERTY_1_VALUE = "startupProperty1Value";
    private String STARTUP_PROPERTY_2_VALUE = "startupProperty2Value";

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/config/startup-properties-test.xml";
    }

    @Override
    protected Properties getStartUpProperties()
    {
        Properties p = new Properties();
        p.setProperty(STARTUP_PROPERTY_1_KEY, STARTUP_PROPERTY_1_VALUE);
        p.setProperty(STARTUP_PROPERTY_2_KEY, STARTUP_PROPERTY_2_VALUE);
        return p;
    }

    @Test
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
