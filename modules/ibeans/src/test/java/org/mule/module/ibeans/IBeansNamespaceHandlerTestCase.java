/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.ibean.IBeansConnector;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IBeansNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        //TODO You'll need to edit this file to configure the properties specific to your transport
        return "ibeans-namespace-config.xml";
    }

    @Test
    public void testIbeansConfig() throws Exception
    {
        IBeansConnector c = (IBeansConnector) muleContext.getRegistry().lookupConnector("ibeansConnector");
        assertNotNull(c);
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());

        //TODO Assert specific properties are configured correctly
    }

}
