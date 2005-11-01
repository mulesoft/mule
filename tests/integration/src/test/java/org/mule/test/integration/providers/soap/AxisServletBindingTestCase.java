/* 
* $Header$
* $Revision$
* $Date$
* ------------------------------------------------------------------------------------------------------
* 
* Copyright (c) SymphonySoft Limited. All rights reserved.
* http://www.symphonysoft.com
* 
* The software in this package is published under the terms of the BSD
* style license a copy of which has been included with this distribution in
* the LICENSE.txt file. 
*
*/
package org.mule.test.integration.providers.soap;

import org.mortbay.http.HttpContext;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.util.InetAddrPort;
import org.mule.extras.client.MuleClient;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.servlet.MuleReceiverServlet;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AxisServletBindingTestCase extends AbstractSoapFunctionalTestCase
{
    private Server httpServer;
    public static final int HTTP_PORT = 8081;

    protected void doPostFunctionalSetUp() throws Exception
    {
        httpServer = new Server();
        SocketListener socketListener = new SocketListener(new InetAddrPort(HTTP_PORT));
        httpServer.addListener(socketListener);

        HttpContext context = httpServer.getContext("/");
        context.setRequestLog(null);

        ServletHandler handler = new ServletHandler();
        handler.addServlet("MuleReceiverServlet",  "/services/*", MuleReceiverServlet.class.getName());

        context.addHandler(handler);
        httpServer.start();
    }

    String getProtocol() {
        return "axis";
    }

    protected void doFunctionalTearDown() throws Exception {
        httpServer.stop();
    }

    public String getConfigResources() {
        return "org/mule/test/integration/providers/soap/axis-test-servlet-mule-config.xml";
    }

    protected String getRequestResponseEndpoint() {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=echo";
    }

    protected String getReceiveEndpoint() {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getDate";
    }

    protected String getReceiveComplexEndpoint() {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getPerson&param=Fred";
    }

    protected String getSendReceiveComplexEndpoint1() {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=addPerson";
    }

    protected String getSendReceiveComplexEndpoint2() {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getPerson&param=Ross";
    }

    protected String getReceiveComplexCollectionEndpoint() {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getPeople";
    }

    protected String getDispatchAsyncComplexEndpoint1() {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=addPerson";
    }

    protected String getDispatchAsyncComplexEndpoint2() {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getPerson&param=Joe";
    }

    protected String getTestExceptionEndpoint() {
        return "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getDate";
    }

    public void testLocationUrlInWSDL() throws Exception {
        Map props = new HashMap();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("http://localhost:" + HTTP_PORT + "/services/mycomponent?wsdl", null, props);
        assertNotNull(result);
        assertTrue(result.getPayloadAsString().indexOf("location=\"http://localhost:" + HTTP_PORT + "/services/mycomponent") > -1);
    }
}