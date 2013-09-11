/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
