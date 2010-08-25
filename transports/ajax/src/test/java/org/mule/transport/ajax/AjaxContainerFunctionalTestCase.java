/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ajax;

import org.mule.transport.ajax.container.MuleAjaxServlet;
import org.mule.transport.servlet.MuleServletContextListener;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

public class AjaxContainerFunctionalTestCase extends AjaxFunctionalTestCase
{
    private Server httpServer;

    @Override
    protected String getConfigResources()
    {
        return "ajax-container-functional-test.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        httpServer = new Server(SERVER_PORT);

        Context c = new Context(httpServer, "/", Context.SESSIONS);
        c.addServlet(new ServletHolder(new MuleAjaxServlet()), "/ajax/*");
        c.addEventListener(new MuleServletContextListener(muleContext, null)); 

        httpServer.start();

        super.doSetUp();

    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        if(httpServer!=null) httpServer.stop();

    }


}
