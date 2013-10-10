/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.service.Service;
import org.mule.construct.Flow;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class InheritedPropertiesMule2458TestCase extends AbstractServiceAndFlowTestCase
{
    public InheritedPropertiesMule2458TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/spring/inherited-properties-mule-2458-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/spring/inherited-properties-mule-2458-test-flow.xml"}
        });
    }      
    
    @Test
    public void testProperties()
    {
        ImmutableEndpoint endpoint;
        Object service = muleContext.getRegistry().lookupObject("service");
        assertNotNull(service);
               
        if (variant.equals(ConfigVariant.FLOW))
        {
            endpoint = (ImmutableEndpoint) ((Flow)service).getMessageSource();    
        }
        else
        {
            endpoint = ((ServiceCompositeMessageSource) ((Service) service).getMessageSource()).getEndpoints().get(0);
        }       
               
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
