/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.providers.soap;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transport.DispatchException;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.services.Person;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    protected abstract String getWsdlEndpoint();

    protected Map getMessageProperties(Map props)
    {
        return props;
    }

    public void testRequestResponse() throws Throwable
    {
        MuleClient client = new MuleClient();
        List results = new ArrayList();
        int number = 1;
        Map props = new HashMap();
        for (int i = 0; i < number; i++)
        {
            props.put("X-Message-Number", String.valueOf(i));
            MuleMessage msg = client.send(getRequestResponseEndpoint(), "Message " + i, getMessageProperties(props));
            assertNotNull(msg);
            results.add(msg.getPayload());
        }

        assertEquals(number, results.size());
        for (int i = 0; i < number; i++)
        {
            assertEquals("Message " + i, results.get(i).toString());
        }
    }

    public void testRequest() throws Throwable
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.request(getReceiveEndpoint(), 0);
        assertNotNull(result);
        assertNotNull(result.getPayload());
        assertTrue(result.getPayload().toString().length() > 0);
    }

    public void testReceiveComplex() throws Throwable
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.request(getReceiveComplexEndpoint(), 0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Fred", ((Person)result.getPayload()).getFirstName());
        assertEquals("Flintstone", ((Person)result.getPayload()).getLastName());

        result = client.request(getReceiveComplexEndpoint(), 0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Fred", ((Person)result.getPayload()).getFirstName());
        assertEquals("Flintstone", ((Person)result.getPayload()).getLastName());
    }

    public void testSendAndReceiveComplex() throws Throwable
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send(getSendReceiveComplexEndpoint1(), new Person("Dino", "Flintstone"),
            getMessageProperties(new HashMap()));
        assertEquals(NullPayload.getInstance(), result.getPayload());

        result = client.request(getSendReceiveComplexEndpoint2(), 0);
        assertNotNull(result);
        
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Dino", ((Person)result.getPayload()).getFirstName());
        assertEquals("Flintstone", ((Person)result.getPayload()).getLastName());
    }

    public void testReceiveComplexCollection() throws Throwable
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.request(getReceiveComplexCollectionEndpoint(), 0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person[]);
        assertEquals(3, ((Person[])result.getPayload()).length);
    }

    public void testDispatchAsyncComplex() throws Throwable
    {
        MuleClient client = new MuleClient();

        client.dispatch(getDispatchAsyncComplexEndpoint1(), new Person("Betty", "Rubble"), getMessageProperties(new HashMap()));
        Thread.sleep(4500);

        // lets get our newly added person
        MuleMessage result = client.request(getDispatchAsyncComplexEndpoint2(), 0);
        assertNotNull(result);
        assertTrue("Did not receive a Person but: " + result.getPayload().getClass(),
            result.getPayload() instanceof Person);
        assertEquals("Betty", ((Person)result.getPayload()).getFirstName());
        assertEquals("Rubble", ((Person)result.getPayload()).getLastName());
    }

    public void testException() throws Exception
    {
        MuleClient client = new MuleClient();
        try
        {
            client.send(getTestExceptionEndpoint(), new Person("Ross", "Mason"), getMessageProperties(new HashMap()));
            fail("A nested Fault should have been raised");
        }
        catch (MuleException e)
        {
            // toplevel
            assertTrue(e instanceof DispatchException);
            // the nested Axis fault
            assertTrue(e.getCause() instanceof Exception);
        }
    }

    public void testLocationUrlInWSDL() throws Exception
    {
        Map props = new HashMap();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
        MuleClient client = new MuleClient();
        MuleMessage result = client.send(getWsdlEndpoint(), null, props);
        assertNotNull(result);
        if (logger.isDebugEnabled())
        {
            logger.debug(result.getPayloadAsString());
        }

        String location = getWsdlEndpoint();
        location = location.substring(0, location.length() - 5);
        if (location.endsWith("/"))
        {
            location = location.substring(0, location.length() - 1);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(result.getPayloadAsString());
        }
        System.out.println(result.getPayloadAsString());
        if (result.getPayloadAsString().indexOf("location=\"" + location) == -1)
        {
            assertTrue(result.getPayloadAsString().indexOf("location='" + location) > -1);
        }
        else
        {
            assertTrue(result.getPayloadAsString().indexOf("location=\"" + location) > -1);
        }

        assertTrue(result.getStringProperty(HttpConstants.HEADER_CONTENT_TYPE, "").startsWith("text/xml"));
    }

}
