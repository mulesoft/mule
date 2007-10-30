/*
 * $Id: XFireConnectorTestCase.java 3903 2006-11-17 21:05:19Z holger $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import org.mule.providers.cxf.CxfConnector;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

import org.apache.cxf.message.MessageImpl;

public class CxfConnectorTestCase extends AbstractConnectorTestCase
{
    public String getTestEndpointURI()
    {
        return "cxf:http://localhost:38009/cxf";
    }

    public Object getValidMessage() throws Exception
    {
        return new MessageImpl();
    }

    @Override
    public UMOConnector createConnector() throws Exception
    {
        CxfConnector c = new CxfConnector();
        c.setManagementContext(managementContext);
        c.setName("cxfConnector");
        return c;
    }
}
