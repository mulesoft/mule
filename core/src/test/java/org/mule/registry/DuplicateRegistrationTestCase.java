/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.registry;

import org.mule.api.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.StringUtils;

import java.util.Collection;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DuplicateRegistrationTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testComponentAlreadyDefinedThrowsException() throws Exception
    {
        Collection components = muleContext.getRegistry().lookupServices();
        assertEquals(0, components.size());
        
        final String componentName = "TEST_COMPONENT_1";
        getTestService(componentName, Object.class);

        components = muleContext.getRegistry().lookupServices();
        assertEquals(1, components.size());
        
        // register it again with the same name
        try
        {
            getTestService(componentName, Object.class);
            fail("Trying to register a service with the same name must have thrown an exception.");
        }
        catch (RegistrationException e)
        {
            // expected
            assertTrue("Exception message should contain service name", 
                       StringUtils.contains(e.getMessage(), componentName));
        }

        components = muleContext.getRegistry().lookupServices();
        assertEquals(1, components.size());
    }
}
