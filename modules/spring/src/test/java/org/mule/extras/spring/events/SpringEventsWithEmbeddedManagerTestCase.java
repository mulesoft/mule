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

import org.mule.config.MuleProperties;

import java.util.Map;

public class SpringEventsWithEmbeddedManagerTestCase extends SpringEventsTestCase
{

    protected String getConfigResources()
    {
        return "../../../../../../../../../../tests/functional/src/test/resources/org/mule/test/spring/mule-events-app-with-embedded-manager.xml";
    }

    public void testCorrectManagerLoaded()
    {
        Map props = (Map)managementContext.getRegistry().lookupObject(MuleProperties.OBJECT_MULE_APPLICATION_PROPERTIES);
        assertNotNull(props);
        assertNotNull(props.get("embeddedManager"));
    }
}
