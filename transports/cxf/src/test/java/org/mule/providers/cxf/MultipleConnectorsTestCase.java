/*
 * $Id: XFireMultipleConnectorsTestCase.java 5750 2007-03-21 12:44:43Z Lajos $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class MultipleConnectorsTestCase extends FunctionalTestCase
{

    public MultipleConnectorsTestCase()
    {
        super();
        this.setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "multiple-connectors.xml";
    }

    public void testCxfConnector1() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply = client.send("cxf1", new MuleMessage("mule"));
        assertEquals("Received: mule", reply.getPayloadAsString());
    }

    public void testCxfConnector2() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply = client.send("cxf2", new MuleMessage("mule"));
        assertEquals("Received: mule", reply.getPayloadAsString());
    }

}
