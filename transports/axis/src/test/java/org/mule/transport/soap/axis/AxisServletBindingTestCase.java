/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis;

import org.mule.tck.providers.soap.AbstractSoapFunctionalTestCase;
import org.mule.transport.http.servlet.MuleReceiverServlet;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

public class AxisServletBindingTestCase extends AbstractSoapFunctionalTestCase
{
    public static final int HTTP_PORT = 62088;

    private Server httpServer;

    // @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        httpServer = new Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(HTTP_PORT);
        httpServer.addConnector(connector);

        Context context = new Context();
        context.setContextPath("/");

        ServletHolder holder = new ServletHolder();
        holder.setServlet(new MuleReceiverServlet());
        context.addServlet(holder, "/services/*");

        httpServer.addHandler(context);
        httpServer.start();
    }

    // @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        // this generates an exception in GenericServlet which we can safely ignore
        if (httpServer != null)
        {
            httpServer.stop();
            httpServer.destroy();
        }
    }

    public String getConfigResources()
    {
        return "axis-test-servlet-mule-config.xml";
    }

    protected String getRequestResponseEndpoint()
    {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=echo";
    }

    protected String getReceiveEndpoint()
    {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getDate";
    }

    protected String getReceiveComplexEndpoint()
    {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getPerson&param=Fred";
    }

    protected String getSendReceiveComplexEndpoint1()
    {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=addPerson";
    }

    protected String getSendReceiveComplexEndpoint2()
    {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getPerson&param=Dino";
    }

    protected String getReceiveComplexCollectionEndpoint()
    {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getPeople";
    }

    protected String getDispatchAsyncComplexEndpoint1()
    {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=addPerson";
    }

    protected String getDispatchAsyncComplexEndpoint2()
    {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getPerson&param=Betty";
    }

    protected String getTestExceptionEndpoint()
    {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getDate";
    }

    protected String getWsdlEndpoint()
    {
        return "http://localhost:" + HTTP_PORT + "/services/mycomponent?wsdl";
    }
}
