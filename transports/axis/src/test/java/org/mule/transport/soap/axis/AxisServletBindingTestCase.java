/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    public String getConfigResources()
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
