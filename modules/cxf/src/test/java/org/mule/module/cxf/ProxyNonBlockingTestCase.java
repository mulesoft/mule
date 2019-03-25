/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.SensingNullRequestResponseMessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

public class ProxyNonBlockingTestCase extends FunctionalTestCase
{

    private static final String SERVICES_ECHO_PATH = "/services/echo";

    private static final String PROXIES_ECHO_PATH = "/proxies/echo";

    private static final String SERVICES_GREETER_PATH = "/services/greeter";

    private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(POST.name()).disableStatusCodeValidation().build();

    private CountDownLatch latch = new CountDownLatch(NUM_REQUESTS);

    private static final int NUM_REQUESTS = 100;

    private static final String ECHO_SOAP_REQUEST =
        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
            "<soap:Body><test xmlns=\"http://foo\"> foo </test></soap:Body>" +
        "</soap:Envelope>";

    private static final String GREETER_SOAP_TEST_ELEMENT_REQUEST = "<greetMe xmlns=\"http://apache.org/hello_world_soap_http/types\"><requestType>Dan</requestType></greetMe>";
    private static final String GREETER_SOAP_TEST_ELEMENT_RESPONSE = "<greetMeResponse xmlns=\"http://apache.org/hello_world_soap_http/types\"><responseType>Hello Dan</responseType></greetMeResponse>";

    private static final String GREETER_SOAP_REQUEST =
        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
            "<soap:Body>" + GREETER_SOAP_TEST_ELEMENT_REQUEST + "</soap:Body>" +
        "</soap:Envelope>";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public AtomicInteger responseCount = new AtomicInteger(0);

    @Override
    protected String getConfigFile()
    {
        return "proxy-conf-flow-httpn-nb.xml";
    }

    @Test
    public void testEchoService() throws Exception
    {
        doTest(SERVICES_ECHO_PATH, ECHO_SOAP_REQUEST, ECHO_SOAP_REQUEST);
    }

    @Test
    public void testEchoProxy() throws Exception
    {
        doTest(PROXIES_ECHO_PATH, ECHO_SOAP_REQUEST, ECHO_SOAP_REQUEST);
        getSensingInstance("sensingRequestResponseProcessorEcho").assertRequestResponseThreadsDifferent();
    }

    @Test
    public void testGreeterService() throws Exception
    {
        doTest(SERVICES_GREETER_PATH, GREETER_SOAP_REQUEST, GREETER_SOAP_TEST_ELEMENT_RESPONSE);
    }

    @Test
    public void testRaceConditionOnNonBlockingProxy() throws Exception
    {
        Set<Thread> threads = new HashSet<Thread>();

        for (int i = 0; i < NUM_REQUESTS; i++)
        {
            Thread thread = new Thread(new TestForRaceConditionRunnable(), "thread" + i);
            threads.add(thread);
            thread.start();
        }

        latch.await();
        assertThat(responseCount.get(), CoreMatchers.equalTo(NUM_REQUESTS));
    }


    @Test
    public void testGreeterProxy() throws Exception
    {
        doTest("/proxies/greeter", GREETER_SOAP_REQUEST, GREETER_SOAP_TEST_ELEMENT_RESPONSE);
        getSensingInstance("sensingRequestResponseProcessorGreeter").assertRequestResponseThreadsDifferent();
    }

    private void doTest(String path, String request, String expectedResponse) throws Exception
    {
        assertThat(getTestResultString(path, request), containsString(expectedResponse));
    }

    private String getTestResultString(String path, String request) throws MuleException, Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + path, getTestMuleMessage(request), HTTP_REQUEST_OPTIONS);
        return result.getPayloadAsString();
    }

    private SensingNullRequestResponseMessageProcessor getSensingInstance(String instanceBeanName)
    {
        return ((SensingNullRequestResponseMessageProcessor) muleContext.getRegistry().lookupObject(instanceBeanName));
    }

    private class TestForRaceConditionRunnable implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                if (getTestResultString(PROXIES_ECHO_PATH, ECHO_SOAP_REQUEST).contains(ECHO_SOAP_REQUEST))
                {
                    responseCount.getAndIncrement();
                }

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                latch.countDown();
            }
        }
    }

}
