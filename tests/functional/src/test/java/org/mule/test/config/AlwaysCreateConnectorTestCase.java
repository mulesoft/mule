/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.MuleManager;
import org.mule.tck.FunctionalTestCase;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AlwaysCreateConnectorTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "always-create-connector-config.xml";
    }

    public void testConnectorConfig() throws Exception
    {
        assertEquals(2, MuleManager.getInstance().getConnectors().size());
    }
}
