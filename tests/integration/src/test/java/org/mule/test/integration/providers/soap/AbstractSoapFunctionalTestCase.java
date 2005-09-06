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
package org.mule.test.integration.providers.soap;

import org.mule.extras.client.MuleClient;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.service.ConnectorFactory;
import org.mule.tck.FunctionalTestCase;
import org.mule.test.integration.service.Person;
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
public abstract class AbstractSoapFunctionalTestCase extends FunctionalTestCase
{

    protected abstract String getRequestResponseEndpoint();
    
    protected abstract String getReceiveEndpoint();
    
    protected abstract String getReceiveComplexEndpoint();
    
    protected abstract String getSendReceiveComplexEndpoint1();
    
    protected abstract String getSendReceiveComplexEndpoint2();
    
    protected abstract String getReceiveComplexCollectionEndpoint();
    
    protected abstract String getDispatchAsyncComplexEndpoint1();
    
    protected abstract String getDispatchAsyncComplexEndpoint2();

    protected abstract String getTestExceptionEndpoint();

    protected String getProtocol() {
        return "axis";
    }

    public void testRequestResponse() throws Throwable
    {
        MuleClient client = new MuleClient();

        List results = new ArrayList();
        int number = 100;
        for (int i = 0; i < number; i++) {
            results.add(client.send(getProtocol() + ":" + getRequestResponseEndpoint(), "Message " + i, null).getPayload());
        }

        assertEquals(number, results.size());
        for (int i = 0; i < number; i++) {
            assertEquals("Message " + i, results.get(i).toString());
        }
    }

    public void testReceive() throws Throwable
    {
        UMOConnector c = ConnectorFactory.getConnectorByProtocol(getProtocol());
        assertNotNull(c);
        UMOMessageDispatcher dispatcher = c.getDispatcher("ANY");
        UMOMessage result = dispatcher.receive(new MuleEndpointURI(getReceiveEndpoint()),
                                               0);
        assertNotNull(result);
        assertNotNull(result.getPayload());
        assertTrue(result.getPayload().toString().length() > 0);
    }

    public void testReceiveComplex() throws Throwable
    {
        UMOConnector c = ConnectorFactory.getConnectorByProtocol(getProtocol());
        assertNotNull(c);
        UMOMessageDispatcher dispatcher = c.getDispatcher("ANY");
        UMOMessage result = dispatcher.receive(new MuleEndpointURI(getReceiveComplexEndpoint()),
                                               0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Fred", ((Person) result.getPayload()).getFirstName());
        assertEquals("Flintstone", ((Person) result.getPayload()).getLastName());
    }

    public void testSendComplex() throws Throwable
    {
        UMOConnector c = ConnectorFactory.getConnectorByProtocol(getProtocol());
        assertNotNull(c);
        UMOMessageDispatcher dispatcher = c.getDispatcher("ANY");
        UMOEndpoint endpoint = new MuleEndpoint("test",
                                                new MuleEndpointURI(getSendReceiveComplexEndpoint1()),
                                                c,
                                                null,
                                                UMOEndpoint.ENDPOINT_TYPE_SENDER,
                                                0,
                                                null);
        UMOEvent event = getTestEvent(new Person("Ross", "Mason"), endpoint);

        UMOMessage result = dispatcher.send(event);
        assertNull(result);

        // lets get our newly added person
        result = dispatcher.receive(new MuleEndpointURI(getSendReceiveComplexEndpoint2()),
                                    0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Ross", ((Person) result.getPayload()).getFirstName());
        assertEquals("Mason", ((Person) result.getPayload()).getLastName());
    }

    public void testReceiveComplexCollection() throws Throwable
    {
        UMOConnector c = ConnectorFactory.getConnectorByProtocol(getProtocol());
        assertNotNull(c);
        UMOMessageDispatcher dispatcher = c.getDispatcher("ANY");
        UMOMessage result = dispatcher.receive(new MuleEndpointURI(getReceiveComplexCollectionEndpoint()),
                                               0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person[]);
        assertEquals(3, ((Person[]) result.getPayload()).length);
    }

    public void testDispatchAsyncComplex() throws Throwable
    {
        UMOConnector c = ConnectorFactory.getConnectorByProtocol(getProtocol());
        assertNotNull(c);
        UMOMessageDispatcher dispatcher = c.getDispatcher("ANY");
        UMOEndpoint endpoint = new MuleEndpoint("test",
                                                new MuleEndpointURI(getDispatchAsyncComplexEndpoint1()),
                                                c,
                                                null,
                                                UMOEndpoint.ENDPOINT_TYPE_SENDER,
                                                0,
                                                null);
        UMOEvent event = getTestEvent(new Person("Joe", "Blow"), endpoint);

        dispatcher.dispatch(event);
        Thread.sleep(2000);
        // lets get our newly added person
        UMOMessage result = dispatcher.receive(new MuleEndpointURI(getDispatchAsyncComplexEndpoint2()),
                                               0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Joe", ((Person) result.getPayload()).getFirstName());
        assertEquals("Blow", ((Person) result.getPayload()).getLastName());
    }

    public void testException() throws Throwable
    {
        UMOConnector c = ConnectorFactory.getConnectorByProtocol(getProtocol());
        assertNotNull(c);
        UMOMessageDispatcher dispatcher = c.getDispatcher("ANY");
        UMOEndpoint endpoint = new MuleEndpoint("test",
                                                new MuleEndpointURI(getTestExceptionEndpoint()),
                                                c,
                                                null,
                                                UMOEndpoint.ENDPOINT_TYPE_SENDER,
                                                0,
                                                null);
        UMOEvent event = getTestEvent(new Person("Nodet", "Guillaume"), endpoint);
        try {
            dispatcher.send(event);
            fail("An Fault should have been raised");
        } catch (Exception f) {
            // This is ok
        }
    }

}
