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

import org.mule.transport.servlet.MuleReceiverServlet;
import org.mule.transport.servlet.jetty.util.EmbeddedJettyServer;

public class AxisServletBindingTestCase extends AbstractSoapFunctionalTestCase
{
    public static final int HTTP_PORT = 62088;

    private EmbeddedJettyServer httpServer;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        httpServer = new EmbeddedJettyServer(HTTP_PORT, "/", "/services/*", new MuleReceiverServlet(), muleContext);
        httpServer.start();
    }

    @Override
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

    @Override
    public String getConfigResources()
    {
        return "axis-test-servlet-mule-config.xml";
    }

    @Override
    protected String getRequestResponseEndpoint()
    {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=echo";
    }

    @Override
    protected String getReceiveEndpoint()
    {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getDate";
    }

    @Override
    protected String getReceiveComplexEndpoint()
    {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getPerson&param=Fred";
    }

    @Override
    protected String getSendReceiveComplexEndpoint1()
    {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=addPerson";
    }

    @Override
    protected String getSendReceiveComplexEndpoint2()
    {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getPerson&param=Dino";
    }

    @Override
    protected String getReceiveComplexCollectionEndpoint()
    {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getPeople";
    }

    @Override
    protected String getDispatchAsyncComplexEndpoint1()
    {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=addPerson";
    }

    @Override
    protected String getDispatchAsyncComplexEndpoint2()
    {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getPerson&param=Betty";
    }

    @Override
    protected String getTestExceptionEndpoint()
    {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getDate";
    }

    @Override
    protected String getWsdlEndpoint()
    {
        return "http://localhost:" + HTTP_PORT + "/services/mycomponent?wsdl";
    }
}
