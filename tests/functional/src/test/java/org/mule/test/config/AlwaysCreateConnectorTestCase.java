/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.tck.FunctionalTestCase;

public class AlwaysCreateConnectorTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "always-create-connector-config.xml";
    }

    public void testConnectorConfig() throws Exception
    {
        assertEquals(2, managementContext.getRegistry().getConnectors().size());
    }
    
}
