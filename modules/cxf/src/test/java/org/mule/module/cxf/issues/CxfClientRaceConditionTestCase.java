/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.issues;

import static javax.jms.DeliveryMode.NON_PERSISTENT;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import static org.mule.transport.http.HttpConnector.HTTP;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.size.SmallTest;
import org.mule.util.IOUtils;

import java.io.InputStream;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQBytesMessage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@SmallTest
public class CxfClientRaceConditionTestCase extends FunctionalTestCase
{
    private static final String HTTP_METHOD = "httpTest";

    private static final String HTTP_HOST = "localhost";

    private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(org.mule.module.http.api.HttpConstants.Methods.POST.name()).disableStatusCodeValidation().build();

    private static final String EXPECTED_JMS_MESSAGE =
            "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\"><soap:Body/></soap:Envelope>";

    private static final String HTTP_REQUEST_BODY =
            "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:ns=\"http://dummy.org\"><soap:Body/><ns:dummy></ns:dummy></soap:Envelope>";

    private static final String EXPECTED_HTTP_MESSAGE =
            "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\"><soap:Body><ns:dummy xmlns:ns=\"http://dummy.org\"/></soap:Body></soap:Envelope>";

    private static final String INBOUND_TEST_QUEUE = "inboundTest";

    private static final String OUTBOUND_TEST_QUEUE = "cxfTest";

    private static final String JMS_CONNECTION_ADDRESS = "vm://localhost";

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");


    private int totalMessages;

    @Before
    public void setup()
    {
        totalMessages = 10000;
    }

    @Test
    public void testRaceConditionHTTP() throws Exception
    {
        cxfHTTPTest(HTTP, HTTP_HOST, port1.getNumber(), HTTP_METHOD, HTTP_REQUEST_OPTIONS, EXPECTED_HTTP_MESSAGE);
    }

    @Test
    public void testRaceConditionJMS() throws Exception
    {
        cxfJMSTest(INBOUND_TEST_QUEUE, OUTBOUND_TEST_QUEUE, EXPECTED_JMS_MESSAGE);
    }

    private void cxfJMSTest(String inboundQueue, String outboundQueue, String expectedMessage) throws Exception
    {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(JMS_CONNECTION_ADDRESS);

        Connection consumerConnection = connectionFactory.createConnection();
        consumerConnection.start();
        Session consumerSession = consumerConnection.createSession(false, AUTO_ACKNOWLEDGE);
        Destination consumerDestination = consumerSession.createQueue(outboundQueue);
        MessageConsumer consumer = consumerSession.createConsumer(consumerDestination);

        Connection producerConnection = connectionFactory.createConnection();
        producerConnection.start();
        Session producerSession = producerConnection.createSession(false, AUTO_ACKNOWLEDGE);
        Destination producerDestination = producerSession.createQueue(inboundQueue);
        MessageProducer producer = producerSession.createProducer(producerDestination);
        producer.setDeliveryMode(NON_PERSISTENT);

        for (int i = 0; i < totalMessages; i++)
        {
            BytesMessage messageSend = producerSession.createBytesMessage();
            producer.send(messageSend);
            Message messageReceived = consumer.receive(5000);
            ActiveMQBytesMessage bytesMessage = (ActiveMQBytesMessage) messageReceived;
            String messageBody = new String(bytesMessage.getContent().getData());
            String messageTrimmed = messageBody.trim();

            assertThat(messageTrimmed, equalTo(expectedMessage));
        }

        producer.close();
        producerSession.close();
        producerConnection.close();

        consumer.close();
        consumerSession.close();
        consumerConnection.close();
    }

    private void cxfHTTPTest(String protocol, String host, Integer port, String method, HttpRequestOptions options, String expectedMessage) throws MuleException
    {
        for (int i = 0; i < totalMessages; i++)
        {

            MuleMessage request = new DefaultMuleMessage(HTTP_REQUEST_BODY, muleContext);
            MuleClient client = muleContext.getClient();
            MuleMessage message = client.send(protocol + "://" + host + ":" + port + "/" + method, request, options);
            String response = IOUtils.toString((InputStream) message.getPayload());
            assertThat(response, equalTo(expectedMessage));
        }
    }

    @Override
    protected String getConfigResources()
    {
        return "issues/cxf-client-race-condition.xml";
    }

}
