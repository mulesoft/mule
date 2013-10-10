/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpPersistentQueueTestCase extends AbstractServiceAndFlowTestCase
{
    private CountDownLatch messageDidArrive = new CountDownLatch(1);
    private int port = -1;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");
    
    public HttpPersistentQueueTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-persistent-queue-service.xml"},
            {ConfigVariant.FLOW, "http-persistent-queue-flow.xml"}
        });
    }      
    
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent("PersistentQueueAsync");
        assertNotNull(testComponent);
        testComponent.setEventCallback(new Callback(messageDidArrive));
        port = dynamicPort.getNumber();
    }

    @Test
    public void testPersistentMessageDeliveryWithGet() throws Exception
    {
        GetMethod method = new GetMethod("http://localhost:" + port + "/services/Echo?foo=bar");
        method.addRequestHeader(HttpConstants.HEADER_CONNECTION, "close");
        doTestPersistentMessageDelivery(method);
    }

    @Test
    public void testPersistentMessageDeliveryWithPost() throws Exception
    {        
        PostMethod method = new PostMethod("http://localhost:" + port + "/services/Echo");        
        method.addRequestHeader(HttpConstants.HEADER_CONNECTION, "close");
        method.addParameter(new NameValuePair("foo", "bar"));
        doTestPersistentMessageDelivery(method);
    }
    
    private void doTestPersistentMessageDelivery(HttpMethod httpMethod) throws Exception
    {
        HttpClient client = new HttpClient();
        int rc = client.executeMethod(httpMethod);
        
        assertEquals(HttpStatus.SC_OK, rc);
        assertTrue(messageDidArrive.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    }
    
    private static class Callback implements EventCallback
    {
        private CountDownLatch messageDidArrive;
        
        public Callback(CountDownLatch latch)
        {
            super();
            messageDidArrive = latch;
        }
        
        public void eventReceived(MuleEventContext context, Object component) throws Exception
        {
            MuleMessage message = context.getMessage();

            Object httpMethod = message.getInboundProperty("http.method");
            if (HttpConstants.METHOD_GET.equals(httpMethod))
            {
                assertEquals("/services/Echo?foo=bar", message.getPayloadAsString());
            }
            else if (HttpConstants.METHOD_POST.equals(httpMethod))
            {
                assertEquals("foo=bar", message.getPayloadAsString());
            }
            else
            {
                fail("invalid HTTP method : " + httpMethod);
            }
            
            assertEquals("true", message.getInboundProperty(HttpConstants.HEADER_CONNECTION));
            assertEquals("true", message.getInboundProperty(HttpConstants.HEADER_KEEP_ALIVE));
            
            messageDidArrive.countDown();            
        }
    }

}
