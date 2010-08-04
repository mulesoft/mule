/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ibeans;

import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;
import org.mule.transport.ibean.IBeansConnector;

public class IBeansConnectorTestCase extends AbstractConnectorTestCase
{
    @Override
    public Connector createConnector() throws Exception
    {
        /* IMPLEMENTATION NOTE: Create and initialise an instance of your
           connector here. Do not actually call the connect method. */

        IBeansConnector connector = new IBeansConnector(muleContext);
        connector.setName("Test");
        // TODO Set any additional properties on the connector here
        return connector;
    }

    @Override
    public String getTestEndpointURI()
    {
        return "ibean://hostip.getHostInfo";
    }

    @Override
    public Object getValidMessage() throws Exception
    {
        return "123.34.56.7";
    }


    public void testProperties() throws Exception
    {
        // TODO test setting and retrieving any custom properties on the
        // Connector as necessary
    }

    @Override
    public void testConnectorMessageRequesterFactory() throws Exception
    {
        //No support for Message Requester
        assertTrue(true);
    }
}
