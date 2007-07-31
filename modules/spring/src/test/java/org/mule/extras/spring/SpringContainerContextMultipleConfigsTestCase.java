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
import org.mule.umo.manager.UMOContainerContext;


/**
 * Tests the Spring container with more than one config file.
 */
public class SpringContainerContextMultipleConfigsTestCase extends AbstractContainerContextTestCase
{
    public String getConfigResources()
    {
        return "test-application-context.xml,test-application-context-2.xml";
    }

    public void testSecondConfig() throws Exception
    {
        UMOContainerContext container = getContainerContext();
        container.initialise();
        assertNotNull(container);

        Object result = container.getComponent("apple2");
        assertNotNull("Component should exist in container", result);
    }

    public UMOContainerContext getContainerContext() throws ConfigurationException
    {
        return new MuleContainerContext();
    }
}
