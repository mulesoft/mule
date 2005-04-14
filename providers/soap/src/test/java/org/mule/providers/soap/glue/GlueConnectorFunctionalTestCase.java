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
 */
package org.mule.providers.soap.glue;

import electric.proxy.IProxy;
import electric.registry.Registry;
import org.mule.MuleManager;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.service.ConnectorFactory;
import org.mule.providers.soap.Person;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageDispatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GlueConnectorFunctionalTestCase extends AbstractMuleTestCase
{
    protected void setUp() throws Exception
    {
        super.setUp();
        if (MuleManager.isInstanciated()) MuleManager.getInstance().dispose();
        ConfigurationBuilder configBuilder = new MuleXmlConfigurationBuilder();
        configBuilder.configure("glue-test-mule-config.xml");
    }

    protected void tearDown() throws Exception {
        MuleManager.getInstance().dispose();
        super.tearDown();
    }

    public void testRequestResponse() throws Throwable
    {
        IProxy proxy = proxy = Registry.bind( "http://localhost:38004/mycomponent2");
        List results = new ArrayList();
        for(int i = 0;i < 100;i++) {
            results.add(proxy.invoke("echo", new Object[]{new String("Message " + i)}));
        }
        assertEquals(100, results.size());
        for(int i = 0;i < 100;i++) {
            assertEquals("Message " + i, results.get(i).toString());
        }
    }

    public void testReceiveSimple() throws Throwable
    {
        UMOConnector c = ConnectorFactory.getConnectorByProtocol("glue");
        assertNotNull(c);
        UMOMessageDispatcher dispatcher = c.getDispatcher("ANY");
        UMOMessage result = dispatcher.receive(new MuleEndpointURI("http://localhost:38004/mule/services/mycomponent?method=getDate"), 0);
        assertNotNull(result);
        assertNotNull(result.getPayload());
    }

    public void testReceiveComplex() throws Throwable
    {
        UMOConnector c = ConnectorFactory.getConnectorByProtocol("glue");
        assertNotNull(c);
        UMOMessageDispatcher dispatcher = c.getDispatcher("ANY");
        UMOMessage result = dispatcher.receive(new MuleEndpointURI("http://localhost:38004/mule/mycomponent3?method=getPerson&param=Fred"), 0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Fred", ((Person)result.getPayload()).getFirstName());
        assertEquals("Flintstone", ((Person)result.getPayload()).getLastName());
    }

    public void testSendComplex() throws Throwable
    {
        UMOConnector c = ConnectorFactory.getConnectorByProtocol("glue");
        assertNotNull(c);
        UMOMessageDispatcher dispatcher = c.getDispatcher("ANY");
        UMOEndpoint endpoint = new MuleEndpoint("test",
                new MuleEndpointURI("http://localhost:38004/mule/mycomponent3?method=addPerson"),
                c, null, UMOEndpoint.ENDPOINT_TYPE_SENDER, 0, null);
        UMOEvent event = getTestEvent(new Person("Ross", "Mason"), endpoint);

        UMOMessage result = dispatcher.send(event);
        assertNull(result);

        //lets get our newly added person
        result = dispatcher.receive(new MuleEndpointURI("http://localhost:38004/mule/mycomponent3?method=getPerson&param=Ross"), 0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Ross", ((Person)result.getPayload()).getFirstName());
        assertEquals("Mason", ((Person)result.getPayload()).getLastName());
    }

    public void testReceiveComplexCollection() throws Throwable
    {
        UMOConnector c = ConnectorFactory.getConnectorByProtocol("glue");
        assertNotNull(c);
        UMOMessageDispatcher dispatcher = c.getDispatcher("ANY");
        UMOMessage result = dispatcher.receive(new MuleEndpointURI("http://localhost:38004/mule/mycomponent3?method=getPeople"), 0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person[]);
        assertEquals(3, ((Person[])result.getPayload()).length);
    }
}
