/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.impl.MuleDescriptor;
import org.mule.tck.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

public class XFireAddClientServiceInterfaceTestCase extends AbstractMuleTestCase
{
    protected MuleDescriptor descriptor;

    protected XFireConnector connector;

    protected void doSetUp() throws Exception
    {
        connector = new XFireConnector();
        List clientServices = new ArrayList();
        clientServices.add("org.mule.components.simple.EchoService");
        connector.setClientServices(clientServices);
        connector.initialise();
    }

    protected void doTearDown() throws Exception
    {
        if (!connector.isDisposed())
        {
            connector.dispose();
        }
    }

    public void testXfireAddClientServiceInterface() throws Exception
    {
        assertNotNull(connector.getXfire().getServiceRegistry().getService("EchoService"));
    }
}
