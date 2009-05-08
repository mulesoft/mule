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
import org.mule.message.DefaultMuleMessageDTO;
import org.mule.transport.cometd.container.MuleCometdServlet;

import java.util.Map;
import java.util.HashMap;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

public class CometdRPCContainerFunctionalTestCase extends CometdRPCFunctionalTestCase
{
    private Server httpServer;

    @Override
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
        return "comet-container-rpc-test.xml";
    }
}