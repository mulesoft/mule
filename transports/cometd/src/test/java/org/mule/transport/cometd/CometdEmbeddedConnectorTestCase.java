/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.cometd;

import org.mule.transport.AbstractConnectorTestCase;
import org.mule.transport.cometd.embedded.CometdConnector;
import org.mule.api.transport.Connector;

public class CometdEmbeddedConnectorTestCase extends AbstractConnectorTestCase
{
    public Connector createConnector() throws Exception
    {
        CometdConnector c = new CometdConnector();
        c.setName("test");
        return c;
    }

    public Object getValidMessage() throws Exception
    {
        return "{'value1' : 'foo', 'value2' : 'bar'}";
    }

    public String getTestEndpointURI()
    {
        return "cometd:http://0.0.0.0:58080/service/request";
    }
}
