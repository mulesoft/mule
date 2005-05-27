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
package org.mule.providers.soap.axis;

import java.util.ArrayList;
import java.util.List;

import org.apache.axis.AxisFault;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.mule.MuleManager;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.config.i18n.Messages;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.service.ConnectorFactory;
import org.mule.providers.soap.Person;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageDispatcher;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AxisConnectorFunctionalTestCase extends AbstractMuleTestCase
{
    protected void setUp() throws Exception
    {
        super.setUp();
        if (MuleManager.isInstanciated()) MuleManager.getInstance().dispose();
        ConfigurationBuilder configBuilder = new MuleXmlConfigurationBuilder();
        configBuilder.configure("axis-test-mule-config.xml");
    }
	
	static public class ComponentWithoutInterfaces {
		public String echo(String msg) {
			return msg;
		}
	}	
	
	public void testComponentWithoutInterfaces() throws Throwable {
		try {
	        UMOConnector c = ConnectorFactory.getConnectorByProtocol("axis");
	        UMODescriptor descriptor = new MuleDescriptor();
	        descriptor.setExceptionListener(new DefaultExceptionStrategy());
	        descriptor.setName("testComponentWithoutInterfaces");
	        descriptor.setImplementation(ComponentWithoutInterfaces.class.getName());
			UMOEndpoint endpoint = new MuleEndpoint("testIn", new MuleEndpointURI("axis:http://localhost:38011/mule/test"),
					c, null, UMOEndpoint.ENDPOINT_TYPE_RECEIVER, 0, null);
			descriptor.setInboundEndpoint(endpoint);
			MuleManager.getInstance().getModel().registerComponent(descriptor);
			fail();
		} catch (InitialisationException e) {
			assertEquals(Messages.X_MUST_IMPLEMENT_AN_INTERFACE, e.getMessageCode());
		}
	}

    public void testRequestResponse() throws Throwable
    {
        Service service = new Service();
        Call call = (Call) service.createCall();

        String endpoint = "http://localhost:38009/mule/mycomponent";
        call.setTargetEndpointAddress(endpoint);
        call.setOperationName("echo");
        call.setSOAPActionURI(endpoint + "?method=echo");
        List results = new ArrayList();
        for(int i = 0;i < 100;i++) {
            results.add( call.invoke(new Object[]{new String("Message " + i)}));
        }

        assertEquals(100, results.size());
        for(int i = 0;i < 100;i++) {
            assertEquals("Message " + i, results.get(i).toString());
        }
    }

    public void testReceive() throws Throwable
    {
        UMOConnector c = ConnectorFactory.getConnectorByProtocol("axis");
        assertNotNull(c);
        UMOMessageDispatcher dispatcher = c.getDispatcher("ANY");
        UMOMessage result = dispatcher.receive(new MuleEndpointURI("http://localhost:38009/axis/services/mycomponent2?method=getDate"), 0);
        assertNotNull(result);
        assertNotNull(result.getPayload());
        assertTrue(result.getPayload().toString().length() > 0);
    }

    public void testReceiveComplex() throws Throwable
    {
        UMOConnector c = ConnectorFactory.getConnectorByProtocol("axis");
        assertNotNull(c);
        UMOMessageDispatcher dispatcher = c.getDispatcher("ANY");
        UMOMessage result = dispatcher.receive(new MuleEndpointURI("http://localhost:38009/mycomponent3?method=getPerson&param=Fred"), 0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Fred", ((Person)result.getPayload()).getFirstName());
        assertEquals("Flintstone", ((Person)result.getPayload()).getLastName());
    }

    public void testSendComplex() throws Throwable
    {
        UMOConnector c = ConnectorFactory.getConnectorByProtocol("axis");
        assertNotNull(c);
        UMOMessageDispatcher dispatcher = c.getDispatcher("ANY");
        UMOEndpoint endpoint = new MuleEndpoint("test",
                new MuleEndpointURI("http://localhost:38009/mycomponent3?method=addPerson"),
                        c, null, UMOEndpoint.ENDPOINT_TYPE_SENDER, 0, null);
        UMOEvent event = getTestEvent(new Person("Ross", "Mason"), endpoint);

        UMOMessage result = dispatcher.send(event);
        assertNull(result);

        //lets get our newly added person
        result = dispatcher.receive(new MuleEndpointURI("http://localhost:38009/mycomponent3?method=getPerson&param=Ross"), 0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Ross", ((Person)result.getPayload()).getFirstName());
        assertEquals("Mason", ((Person)result.getPayload()).getLastName());
    }

    public void testReceiveComplexCollection() throws Throwable
    {
        UMOConnector c = ConnectorFactory.getConnectorByProtocol("axis");
        assertNotNull(c);
        UMOMessageDispatcher dispatcher = c.getDispatcher("ANY");
        UMOMessage result = dispatcher.receive(new MuleEndpointURI("http://localhost:38009/mycomponent3?method=getPeople"), 0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person[]);
        assertEquals(3, ((Person[])result.getPayload()).length);
    }

    public void testDispatchAsyncComplex() throws Throwable
    {
        UMOConnector c = ConnectorFactory.getConnectorByProtocol("axis");
        assertNotNull(c);
        UMOMessageDispatcher dispatcher = c.getDispatcher("ANY");
        UMOEndpoint endpoint = new MuleEndpoint("test",
                new MuleEndpointURI("http://localhost:38010/mycomponent4?method=addPerson"),
                        c, null, UMOEndpoint.ENDPOINT_TYPE_SENDER, 0, null);
        UMOEvent event = getTestEvent(new Person("Rossco", "Pico"), endpoint);

        dispatcher.dispatch(event);
        Thread.sleep(2000);
        //lets get our newly added person
        UMOMessage result = dispatcher.receive(new MuleEndpointURI("http://localhost:38009/mycomponent3?method=getPerson&param=Rossco"), 0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Rossco", ((Person)result.getPayload()).getFirstName());
        assertEquals("Pico", ((Person)result.getPayload()).getLastName());
    }

    public void testException() throws Throwable
    {
        UMOConnector c = ConnectorFactory.getConnectorByProtocol("axis");
        assertNotNull(c);
        UMOMessageDispatcher dispatcher = c.getDispatcher("ANY");
        UMOEndpoint endpoint = new MuleEndpoint("test",
                new MuleEndpointURI("http://localhost:38009/mycomponent3?method=addPerson"),
                        c, null, UMOEndpoint.ENDPOINT_TYPE_SENDER, 0, null);
        UMOEvent event = getTestEvent(new Person("Nodet", "Guillaume"), endpoint);
        try {
        	UMOMessage result = dispatcher.send(event);
        	fail("An AxisFault should have been raised");
        } catch (AxisFault f) {
        	// This is ok
        }
    }

}