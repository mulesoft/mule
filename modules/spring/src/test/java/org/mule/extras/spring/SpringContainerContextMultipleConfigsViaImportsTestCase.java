/*
 * $Id: SpringContainerContextTestCase.java 3765 2006-10-31 19:38:26Z holger $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring;

import org.mule.config.ConfigurationException;
import org.mule.impl.container.MuleContainerContext;
import org.mule.tck.model.AbstractContainerContextTestCase;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOContainerContext;


/**
 * Tests the Spring container where more than one config file are loaded via
 * <code>&lt;import resource="config.xml"/&gt;</code>. 
 */
public class SpringContainerContextMultipleConfigsViaImportsTestCase extends AbstractContainerContextTestCase
{
    public String getConfigResources()
    {
        return "spring-imports.xml";
    }

    public void testSecondConfig() throws Exception
    {
        UMOContainerContext container = getContainerContext();
        container.initialise();
        assertNotNull(container);

        try
        {
            Object result = container.getComponent("apple2");
            assertNotNull("Component should exist in container", result);
        }
        catch (ObjectNotFoundException e)
        {
            fail("Component should exist in the container");
        }
    }

    public UMOContainerContext getContainerContext() throws ConfigurationException
    {
        return new MuleContainerContext();
    }
}
