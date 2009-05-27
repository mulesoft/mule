/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ajax;

import org.mule.transport.ajax.container.MuleAjaxServlet;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

public class AjaxRPCContainerFunctionalTestCase extends AjaxRPCFunctionalTestCase
{
    private Server httpServer;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        httpServer = new Server(58883);
        Context context = new Context(httpServer, "/", Context.SESSIONS);
        context.addServlet(new ServletHolder(new MuleAjaxServlet()), "/cometd/*");

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
        return "ajax-container-rpc-test.xml";
    }
}