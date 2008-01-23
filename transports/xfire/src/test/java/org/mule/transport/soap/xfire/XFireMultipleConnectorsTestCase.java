/*
 * $Id:XFireMultipleConnectorsTestCase.java 7586 2007-07-19 04:06:50Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.xfire;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

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
        MuleMessage reply = client.send("xfire1", new DefaultMuleMessage("mule"));
        assertEquals("Received: mule", reply.getPayloadAsString());
    }
    
    public void testXFireConnector2() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage reply = client.send("xfire2", new DefaultMuleMessage("mule"));
        assertEquals("Received: mule", reply.getPayloadAsString());
    }

}


