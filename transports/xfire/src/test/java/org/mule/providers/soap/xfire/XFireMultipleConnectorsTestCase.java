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

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class XFireMultipleConnectorsTestCase extends FunctionalTestCase
{

    public XFireMultipleConnectorsTestCase()
    {
        super();
        this.setDisposeManagerPerSuite(true);
    }
    
    protected String getConfigResources()
    {
        return "xfire-multiple-connectors.xml";
    }
    
    public void testXFireConnector1() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply = client.send("xfire1", new MuleMessage("mule"));
        assertEquals("Received: mule", reply.getPayloadAsString());
    }
    
    public void testXFireConnector2() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply = client.send("xfire2", new MuleMessage("mule"));
        assertEquals("Received: mule", reply.getPayloadAsString());
    }

}


