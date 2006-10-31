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

public class SpringEventsWithEmbeddedManagerTestCase extends SpringEventsTestCase
{

    protected String getConfigResources()
    {
        return "mule-events-app-with-embedded-manager.xml";
    }

    public void testCorrectManagerLoaded()
    {
        assertNotNull(MuleManager.getInstance().getProperty("embeddedManager"));
    }
}
