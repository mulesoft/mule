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
import org.mule.MuleManager;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.providers.http.servlet.MuleReceiverServlet;
import org.mule.providers.soap.Person;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AxisServletBindingTestCase extends AbstractMuleTestCase
{
    private Server httpServer;
    public static final int HTTP_PORT = 8081;

    protected void setUp() throws Exception
    {
        super.setUp();
        if (MuleManager.isInstanciated())
            MuleManager.getInstance().dispose();
        ConfigurationBuilder configBuilder = new MuleXmlConfigurationBuilder();
        configBuilder.configure("org/mule/test/integration/providers/soap/axis-test-servlet-mule-config.xml");

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

    protected void tearDown() throws Exception {
        httpServer.stop();
    }

    public void testRequestResponse() throws Throwable
    {
        MuleClient client = new MuleClient();
        String uri = "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=echo";
        List results = new ArrayList();
        UMOMessage result;
        for (int i = 0; i < 100; i++) {
            result = client.send(uri, "Message " + i, null);
            results.add(result.getPayload());
        }

        assertEquals(100, results.size());
        for (int i = 0; i < 100; i++) {
            assertEquals("Message " + i, results.get(i).toString());
        }
    }

    public void testReceive() throws Throwable
    {
        MuleClient client = new MuleClient();
        String uri = "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getDate";
        UMOMessage result = client.receive(uri, 0);
        assertNotNull(result);
        assertNotNull(result.getPayload());
        assertTrue(result.getPayload().toString().length() > 0);
    }

    public void testReceiveComplex() throws Throwable
    {
        MuleClient client = new MuleClient();
        String uri = "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getPerson&param=Fred";
        UMOMessage result = client.receive(uri, 0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Fred", ((Person) result.getPayload()).getFirstName());
        assertEquals("Flintstone", ((Person) result.getPayload()).getLastName());
    }

    public void testSendComplex() throws Throwable
    {
        MuleClient client = new MuleClient();
        String uri = "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=addPerson";
        UMOMessage result = client.send(uri, new Person("Ross", "Mason"), null);

        // lets get our newly added person
        uri = "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getPerson&param=Ross";
        result = client.receive(uri, 0);

        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Ross", ((Person) result.getPayload()).getFirstName());
        assertEquals("Mason", ((Person) result.getPayload()).getLastName());
    }

    public void testReceiveComplexCollection() throws Throwable
    {
        MuleClient client = new MuleClient();
        String uri = "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getPeople";
        UMOMessage result = client.receive(uri, 0);

        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person[]);
        assertEquals(3, ((Person[]) result.getPayload()).length);
    }

    public void testDispatchAsyncComplex() throws Throwable
    {
        MuleClient client = new MuleClient();
        String uri = "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=addPerson";
        client.dispatch(uri, new Person("Joe", "Blow"), null);

        Thread.sleep(2000);
        // lets get our newly added person
        uri = "axis:http://localhost:" + HTTP_PORT + "/services/mycomponent?method=getPerson&param=Joe";
        UMOMessage result = client.receive(uri, 0);

        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Joe", ((Person) result.getPayload()).getFirstName());
        assertEquals("Blow", ((Person) result.getPayload()).getLastName());
    }

}