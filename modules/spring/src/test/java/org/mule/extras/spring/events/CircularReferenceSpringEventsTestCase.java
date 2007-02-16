/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.events;

import org.mule.MuleManager;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.manager.UMOManager;

import org.springframework.context.support.AbstractApplicationContext;

public class CircularReferenceSpringEventsTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "../../../../../../../../../../tests/functional/src/test/resources/org/mule/test/spring/mule-events-with-manager.xml";
    }

    public void testManagerIsInstanciated() throws Exception
    {
        UMOManager m = MuleManager.getInstance();
        assertTrue(m.isInitialised());
        assertTrue(m.isStarted());
        assertNotNull(m.getContainerContext());
        assertNotNull(m.getContainerContext().getComponent(
            AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME));
    }
}
