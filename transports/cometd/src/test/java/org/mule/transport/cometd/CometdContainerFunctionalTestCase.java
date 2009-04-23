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

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transport.cometd.container.MuleCometdServlet;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.nio.SelectChannelConnector;

public class CometdContainerFunctionalTestCase extends FunctionalTestCase
{
    private Server httpServer;

    // @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        httpServer = new Server(58883);
        Context context = new Context(httpServer, "/", Context.SESSIONS);
        context.addServlet(new ServletHolder(new MuleCometdServlet()), "/cometd/*");

        httpServer.start();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        if(httpServer!=null) httpServer.stop();
    }

    @Override
    protected String getConfigResources()
    {
        return "comet-container-functional-roundtrip-test.xml";
    }

    public void testDispatchReceiveSimple() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("endpoint1", TEST_MESSAGE, null);

        MuleMessage result = client.request("vm://middle", 5000L);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

    public void testDispatchReceiveComplexObjects() throws Exception
    {
        MuleClient client = new MuleClient();
        FruitBowl bowl =  new FruitBowl(new Apple(), new Banana());
        bowl.getApple().setWashed(true);
        client.dispatch("endpoint2", bowl, null);

        MuleMessage result = client.request("vm://middle2", 5000L);
        assertTrue(result.getPayload() instanceof FruitBowl);
        assertTrue(((FruitBowl)result.getPayload()).hasApple());
        assertTrue(((FruitBowl)result.getPayload()).getApple().isWashed());
        assertTrue(((FruitBowl)result.getPayload()).hasBanana());
    }
}