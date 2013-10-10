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

public class GlobalPropertiesMule2458TestCase extends AbstractServiceAndFlowTestCase
{
    public GlobalPropertiesMule2458TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/spring/global-properties-mule-2458-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/spring/global-properties-mule-2458-test-flow.xml"}
        });
    }      
    
    @Test
    public void testProperties()
    {
        Object service = muleContext.getRegistry().lookupObject("service");
        assertNotNull(service);
        ImmutableEndpoint ep; 
        if (variant.equals(ConfigVariant.FLOW))
        {
            ep = (ImmutableEndpoint) ((Flow) service).getMessageSource();
        }
        else
        {
            ep = ((ServiceCompositeMessageSource) ((Service) service).getMessageSource()).getEndpoints().get(0);    
        }
        
        
        assertNotNull(ep);
        assertEquals("local", ep.getProperties().get("local"));
        assertEquals("global", ep.getProperties().get("global"));
        assertEquals("local", ep.getProperties().get("override-me"));
        assertEquals(3, ep.getProperties().size());
    }
}
