/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;
import org.mule.transport.DefaultMessageAdapter;

public class CxfConnectorTestCase extends AbstractConnectorTestCase
{
    public String getTestEndpointURI()
    {
        return "cxf:http://localhost:38009/cxf";
    }

    public Object getValidMessage() throws Exception
    {
        return new DefaultMessageAdapter("");
    }

    @Override
    public Connector createConnector() throws Exception
    {
        CxfConnector c = new CxfConnector();
        c.setMuleContext(muleContext);
        c.setName("cxfConnector");
        return c;
    }
}
