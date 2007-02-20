/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.models;

import org.mule.MuleManager;
import org.mule.impl.model.direct.DirectModel;
import org.mule.tck.FunctionalTestCase;

public class ConfigureModelTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/models/direct-model-config.xml";
    }

    public void testConfigure()
    {
        assertTrue(managementContext.getRegistry().lookupModel("main") instanceof DirectModel);
        assertEquals("main", managementContext.getRegistry().lookupModel("main").getName());
    }
}
