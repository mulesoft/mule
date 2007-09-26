/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.events;

import org.mule.tck.FunctionalTestCase;

import org.springframework.context.support.AbstractApplicationContext;

public class CircularReferenceSpringEventsTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "mule-events-with-manager.xml";
    }

    public void testManagerIsInstanciated() throws Exception
    {
        assertTrue(managementContext.isInitialised());
        assertTrue(managementContext.isStarted());
        assertNotNull(managementContext.getRegistry().lookupObject(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME));
    }
}
