/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.servlet.MuleReceiverServlet;
import org.mule.transport.servlet.jetty.util.EmbeddedJettyServer;

import org.junit.Rule;

public class AxisServletBindingTestCase extends AbstractSoapFunctionalTestCase
{
    private EmbeddedJettyServer httpServer;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    public String getConfigFile()
    {
        return "axis-test-servlet-mule-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        httpServer = new EmbeddedJettyServer(dynamicPort.getNumber(), "/", "/services/*", new MuleReceiverServlet(), muleContext);
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
    protected String getRequestResponseEndpoint()
    {
        return "axis:http://localhost:" + dynamicPort.getNumber() + "/services/mycomponent?method=echo";
    }

    @Override
    protected String getReceiveEndpoint()
    {
        return "axis:http://localhost:" + dynamicPort.getNumber() + "/services/mycomponent?method=getDate";
    }

    @Override
    protected String getReceiveComplexEndpoint()
    {
        return "axis:http://localhost:" + dynamicPort.getNumber() + "/services/mycomponent?method=getPerson&param=Fred";
    }

    @Override
    protected String getSendReceiveComplexEndpoint1()
    {
        return "axis:http://localhost:" + dynamicPort.getNumber() + "/services/mycomponent?method=addPerson";
    }

    @Override
    protected String getSendReceiveComplexEndpoint2()
    {
        return "axis:http://localhost:" + dynamicPort.getNumber() + "/services/mycomponent?method=getPerson&param=Dino";
    }

    @Override
    protected String getReceiveComplexCollectionEndpoint()
    {
        return "axis:http://localhost:" + dynamicPort.getNumber() + "/services/mycomponent?method=getPeople";
    }

    @Override
    protected String getDispatchAsyncComplexEndpoint1()
    {
        return "axis:http://localhost:" + dynamicPort.getNumber() + "/services/mycomponent?method=addPerson";
    }

    @Override
    protected String getDispatchAsyncComplexEndpoint2()
    {
        return "axis:http://localhost:" + dynamicPort.getNumber() + "/services/mycomponent?method=getPerson&param=Betty";
    }

    @Override
    protected String getTestExceptionEndpoint()
    {
        return "axis:http://localhost:" + dynamicPort.getNumber() + "/services/mycomponent?method=getDate";
    }

    @Override
    protected String getWsdlEndpoint()
    {
        return "http://localhost:" + dynamicPort.getNumber() + "/services/mycomponent?wsdl";
    }
}
