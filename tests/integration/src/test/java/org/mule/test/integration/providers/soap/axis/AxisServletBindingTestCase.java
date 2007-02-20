/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.soap.axis;

import org.mule.providers.http.servlet.MuleReceiverServlet;
import org.mule.test.integration.providers.soap.AbstractSoapFunctionalTestCase;

import org.mortbay.http.HttpContext;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.util.InetAddrPort;

public class AxisServletBindingTestCase extends AbstractSoapFunctionalTestCase
{
    public static final int HTTP_PORT = 18088;

    private Server httpServer;

    // //@Override
    protected void suitePostSetUp() throws Exception
    {
        httpServer = new Server();
        SocketListener socketListener = new SocketListener(new InetAddrPort(HTTP_PORT));
        httpServer.addListener(socketListener);

        HttpContext context = httpServer.getContext("/");
        context.setRequestLog(null);

        ServletHandler handler = new ServletHandler();
        handler.addServlet("MuleReceiverServlet", "/services/*", MuleReceiverServlet.class.getName());

        context.addHandler(handler);
        httpServer.start();
    }

    // //@Override
    protected void suitePostTearDown() throws Exception
    {
        httpServer.stop();
    }

    public String getConfigResources()
    {
        return "org/mule/test/integration/providers/soap/axis/axis-test-servlet-mule-config.xml";
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
