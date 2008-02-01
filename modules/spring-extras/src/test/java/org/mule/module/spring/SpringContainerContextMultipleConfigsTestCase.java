/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.spring;

import org.mule.api.config.ConfigurationException;
import org.mule.api.context.ContainerContext;
import org.mule.container.MuleContainerContext;
import org.mule.context.AbstractContainerContextTestCase;


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
        ContainerContext container = getContainerContext();
        container.initialise();
        assertNotNull(container);

        Object result = container.getComponent("apple2");
        assertNotNull("Service should exist in container", result);
    }

    public ContainerContext getContainerContext() throws ConfigurationException
    {
        return new MuleContainerContext();
    }
}
