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
import org.mule.providers.http.HttpConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.test.integration.service.Person;
import org.mule.umo.UMOMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractSoapFunctionalTestCase extends FunctionalTestCase
{
    protected AbstractSoapFunctionalTestCase() {
        setDisposeManagerPerSuite(true);
    }

    protected abstract String getRequestResponseEndpoint();
    
    protected abstract String getReceiveEndpoint();
    
    protected abstract String getReceiveComplexEndpoint();
    
    protected abstract String getSendReceiveComplexEndpoint1();
    
    protected abstract String getSendReceiveComplexEndpoint2();
    
    protected abstract String getReceiveComplexCollectionEndpoint();
    
    protected abstract String getDispatchAsyncComplexEndpoint1();
    
    protected abstract String getDispatchAsyncComplexEndpoint2();

    protected abstract String getTestExceptionEndpoint();

    protected abstract String getWsdlEndpoint();

    public void testRequestResponse() throws Throwable
    {
        MuleClient client = new MuleClient();

        List results = new ArrayList();
        int number = 1;
        Map props = new HashMap();
        for (int i = 0; i < number; i++) {
            props.put("X-Message-Number", String.valueOf(i));
            UMOMessage msg = client.send(getRequestResponseEndpoint(), "Message " + i, props);
            assertNotNull(msg);
            results.add(msg.getPayload());
        }

        assertEquals(number, results.size());
        for (int i = 0; i < number; i++) {
            assertEquals("Message " + i, results.get(i).toString());
        }
    }

    public void testReceive() throws Throwable
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.receive(getReceiveEndpoint(), 0);
        assertNotNull(result);
        assertNotNull(result.getPayload());
        assertTrue(result.getPayload().toString().length() > 0);
    }

    public void testReceiveComplex() throws Throwable
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.receive(getReceiveComplexEndpoint(), 0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Fred", ((Person) result.getPayload()).getFirstName());
        assertEquals("Flintstone", ((Person) result.getPayload()).getLastName());

        result = client.receive(getReceiveComplexEndpoint(), 0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Fred", ((Person) result.getPayload()).getFirstName());
        assertEquals("Flintstone", ((Person) result.getPayload()).getLastName());
    }

    public void testSendAndReceiveComplex() throws Throwable
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send(getSendReceiveComplexEndpoint1(), new Person("Dino", "Flintstone"), null);
        assertNull(result);

        result = client.receive(getSendReceiveComplexEndpoint2(), 0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Dino", ((Person) result.getPayload()).getFirstName());
        assertEquals("Flintstone", ((Person) result.getPayload()).getLastName());
    }

    public void testReceiveComplexCollection() throws Throwable
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.receive(getReceiveComplexCollectionEndpoint(), 0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person[]);
        assertEquals(4, ((Person[]) result.getPayload()).length);
    }

    public void testDispatchAsyncComplex() throws Throwable
    {
        MuleClient client = new MuleClient();

        client.dispatch(getDispatchAsyncComplexEndpoint1(),new Person("Betty", "Rubble"), null);
        Thread.sleep(4000);

        // lets get our newly added person
        UMOMessage result = client.receive(getDispatchAsyncComplexEndpoint2(), 0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Betty", ((Person) result.getPayload()).getFirstName());
        assertEquals("Rubble", ((Person) result.getPayload()).getLastName());
    }

    public void testException() throws Throwable
    {
        MuleClient client = new MuleClient();
        try {
            client.send(getTestExceptionEndpoint(), new Person("Ross", "Mason"), null);
            fail("An Fault should have been raised");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testLocationUrlInWSDL() throws Exception {
        Map props = new HashMap();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
        MuleClient client = new MuleClient();
        UMOMessage result = client.send(getWsdlEndpoint(), null, props);
        assertNotNull(result);
        System.out.println(result.getPayloadAsString());

        String location = getWsdlEndpoint();
        location = location.substring(0, location.length() - 5);
        if(location.endsWith("/")) {
            location = location.substring(0, location.length() - 1);
        }
        if(result.getPayloadAsString().indexOf("location=\"" + location) == -1) {
            assertTrue(result.getPayloadAsString().indexOf("location='" + location) > -1);
        } else {
            assertTrue(result.getPayloadAsString().indexOf("location=\"" + location) > -1);
        }
        System.out.println(result.getPayloadAsString());
    }
}
