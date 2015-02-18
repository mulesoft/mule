/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.api.processor.MessageProcessor;
import org.mule.context.notification.ExceptionNotification;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;
import org.mule.util.CharSetUtils;
import org.mule.util.concurrent.Latch;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.mutable.MutableInt;
import org.hamcrest.core.IsNull;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class RollbackExceptionStrategyTestCase extends AbstractServiceAndFlowTestCase
{
    public static final int TIMEOUT = 5000;
    public static final String JSON_REQUEST = "{\"userId\":\"15\"}";
    public static final int MAX_REDELIVERY = 4;
    public static final int EXPECTED_DELIVERED_TIMES = MAX_REDELIVERY + 1;
    public static final int SHORT_MAX_REDELIVERY = 2;
    public static final int EXPECTED_SHORT_DELIVERED_TIMES = SHORT_MAX_REDELIVERY + 1;
    public static final String MESSAGE = "some message";
    public static final String MESSAGE_EXPECTED = "some message consumed successfully";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");
    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    public RollbackExceptionStrategyTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        System.setProperty("maxRedelivery", String.valueOf(MAX_REDELIVERY));
        System.setProperty("shortMaxRedelivery", String.valueOf(SHORT_MAX_REDELIVERY));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "org/mule/test/integration/exceptions/rollback-exception-strategy-use-case-service.xml"},
                {ConfigVariant.FLOW, "org/mule/test/integration/exceptions/rollback-exception-strategy-use-case-flow.xml"}});
    }

    @Test
    public void testAlwaysRollback() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(EXPECTED_DELIVERED_TIMES);
        LocalMuleClient client = muleContext.getClient();
        muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>() {
            @Override
            public void onNotification(ExceptionNotification notification)
            {
                latch.countDown();
            }
        });
        client.dispatch("vm://in","some message",null);
        if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS))
        {
            fail("message should have been delivered at least 5 times");
        }
    }
    @Test
    @Ignore("MULE-6926: flaky test")
    public void testAlwaysRollbackJmsNoTransaction() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(EXPECTED_DELIVERED_TIMES);
        LocalMuleClient client = muleContext.getClient();
        muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>() {
            @Override
            public void onNotification(ExceptionNotification notification)
            {
                latch.countDown();
            }
        });
        client.dispatch("jms://in?connector=activeMq","some message",null);
        if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS))
        {
            fail("message should have been delivered at least 5 times");
        }
    }

    @Test
    @Ignore("MULE-6926: flaky test")
    public void testRedeliveryExhaustedTransactional() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(EXPECTED_DELIVERED_TIMES);
        final MutableInt deliveredTimes = new MutableInt(0);
        LocalMuleClient client = muleContext.getClient();
        muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>() {
            @Override
            public void onNotification(ExceptionNotification notification)
            {
                deliveredTimes.increment();
                latch.countDown();
            }
        });
        client.dispatch("jms://in2?connector=activeMq", MESSAGE,null);
        if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS))
        {
            fail("message should have been delivered at least 5 times");
        }
        assertThat(deliveredTimes.intValue(), is(EXPECTED_DELIVERED_TIMES));
        MuleMessage dlqMessage = client.request("jms://dlq?connector=activeMq", TIMEOUT);
        assertThat(dlqMessage, IsNull.<Object>notNullValue());
        assertThat(dlqMessage.getPayloadAsString(), is(MESSAGE_EXPECTED));
    }

	@Test
	public void testRollbackWithComponent() throws Exception
	{
	    final CountDownLatch latch = new CountDownLatch(EXPECTED_DELIVERED_TIMES);
	    LocalMuleClient client = muleContext.getClient();
	    muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>() {
	        @Override
	        public void onNotification(ExceptionNotification notification)
	        {
	            latch.countDown();
	        }
	    });
	    client.dispatch("vm://in5","some message",null);
	    if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS))
	    {
	        fail("message should have been delivered at least 5 times");
	    }
        MuleMessage result = client.send("vm://in5", MESSAGE, null, TIMEOUT);
        assertThat(result,IsNull.<Object>notNullValue());
        assertThat(result.getPayloadAsString(),is(MESSAGE + " Rolled Back"));
	}

    @Test
    public void testFullyDefinedRollbackExceptionStrategyWithComponent() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage result = null;
        for (int i = 1; i <= EXPECTED_SHORT_DELIVERED_TIMES; i++)
        {
            result = client.send("vm://in6", MESSAGE, null, TIMEOUT);
            assertThat(result,IsNull.<Object>notNullValue());
            assertThat(result.getExceptionPayload(),IsNull.<Object>notNullValue());
            assertThat(result.getPayloadAsString(),is(MESSAGE + " apt1 apt2 apt3"));
        }
        result = client.send("vm://in6", MESSAGE, null, TIMEOUT);
        assertThat(result,IsNull.<Object>notNullValue());
        assertThat(result.getPayloadAsString(),is(MESSAGE + " apt4 groovified"));
    }

    @Test
    public void testRedeliveryExhaustedNoTransaction() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(EXPECTED_DELIVERED_TIMES);
        final MutableInt deliveredTimes = new MutableInt(0);
        LocalMuleClient client = muleContext.getClient();
        muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>() {
            @Override
            public void onNotification(ExceptionNotification notification)
            {
                deliveredTimes.increment();
                latch.countDown();
            }
        });
        client.dispatch("jms://in3?connector=activeMq", MESSAGE, null);
        if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS))
        {
            fail("message should have been delivered at least 5 times");
        }
        assertThat(deliveredTimes.intValue(), is(EXPECTED_DELIVERED_TIMES));
        MuleMessage dlqMessage = client.request("jms://dlq?connector=activeMq", TIMEOUT);
        assertThat(dlqMessage, IsNull.<Object>notNullValue());
        assertThat(dlqMessage.getPayloadAsString(), is(MESSAGE_EXPECTED));
    }


    @Test
    public void testHttpAlwaysRollbackUsingMuleClient() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send(String.format("http://localhost:%s", dynamicPort1.getNumber()), getTestMuleMessage(JSON_REQUEST), newOptions().disableStatusCodeValidation().responseTimeout(TIMEOUT).build());
        assertThat(response.<Integer>getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY),is(500));
    }

    @Test
    public void testHttpAlwaysRollbackUsingHttpClient() throws Exception
    {
        HttpClient httpClient = new HttpClient();
        GetMethod getMethod = new GetMethod(String.format("http://localhost:%s", dynamicPort1.getNumber()));
        int status = httpClient.executeMethod(getMethod);
        assertThat(status,is(500));
        getMethod.releaseConnection();
    }

    @Test
    public void testHttpRedeliveryExhaustedRollbackUsingMuleClient() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = null;
        final HttpRequestOptions httpRequestOptions = newOptions().method(POST.name()).disableStatusCodeValidation().responseTimeout(TIMEOUT).build();
        for (int i = 1; i <= EXPECTED_SHORT_DELIVERED_TIMES; i++)
        {
            response = client.send(String.format("http://localhost:%s", dynamicPort2.getNumber()), getTestMuleMessage(MESSAGE), httpRequestOptions);
            assertThat(response.<Integer>getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY),is(500));
        }
        response = client.send(String.format("http://localhost:%s", dynamicPort2.getNumber()),  getTestMuleMessage(MESSAGE), httpRequestOptions);
        assertThat(response.<Integer>getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY),is(200));
        assertThat(response.getExceptionPayload(),IsNull.<Object>nullValue());
        assertThat(response.getPayloadAsString(), is(MESSAGE_EXPECTED));
    }

    @Test
    public void testHttpRedeliveryExhaustedRollbackUsingHttpClient() throws Exception
    {
        HttpClient httpClient = new HttpClient();
        PostMethod postMethod = new PostMethod(String.format("http://localhost:%s", dynamicPort2.getNumber()));
        postMethod.setRequestEntity(new StringRequestEntity(MESSAGE, "html/text", CharSetUtils.defaultCharsetName()));
        int status;
        for (int i = 1; i <= EXPECTED_SHORT_DELIVERED_TIMES; i++)
        {
            status = httpClient.executeMethod(postMethod);
            assertThat(status,is(500));
            postMethod.releaseConnection();
        }
        status = httpClient.executeMethod(postMethod);
        assertThat(status,is(200));
        assertThat(postMethod.getResponseBodyAsString(), is(MESSAGE_EXPECTED));
        postMethod.releaseConnection();
    }

    @Test
    public void testFullyDefinedRollbackExceptionStrategy() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage result = null;
        for (int i = 1; i <= EXPECTED_SHORT_DELIVERED_TIMES; i++)
        {
            result = client.send("vm://in2", MESSAGE, null, TIMEOUT);
            assertThat(result,IsNull.<Object>notNullValue());
            assertThat(result.getExceptionPayload(),IsNull.<Object>notNullValue());
            assertThat(result.getPayloadAsString(),is(MESSAGE + " apt1 apt2 apt3"));
        }
        result = client.send("vm://in2", MESSAGE, null, TIMEOUT);
        assertThat(result,IsNull.<Object>notNullValue());
        assertThat(result.getPayloadAsString(),is(MESSAGE + " apt4 apt5"));
    }

    @Test
    @Ignore("MULE-6926: flaky test")
    public void testRedeliveryPolicyRedefinition() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(EXPECTED_DELIVERED_TIMES);
        final MutableInt deliveredTimes = new MutableInt(0);
        LocalMuleClient client = muleContext.getClient();
        muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>() {
            @Override
            public void onNotification(ExceptionNotification notification)
            {
                deliveredTimes.increment();
                latch.countDown();
            }
        });
        client.dispatch("vm://in3","some message",null);
        if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS))
        {
            fail("message should have been delivered at least 5 times");
        }
        assertThat(deliveredTimes.intValue(),is(EXPECTED_DELIVERED_TIMES));
    }

    @Test
    @Ignore("MULE-6926: flaky test")
    public void testInboundEndpointMaxRedeliveryTakesPrecendence() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(EXPECTED_DELIVERED_TIMES);
        final MutableInt deliveredTimes = new MutableInt(0);
        LocalMuleClient client = muleContext.getClient();
        muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>() {
            @Override
            public void onNotification(ExceptionNotification notification)
            {
                deliveredTimes.increment();
                latch.countDown();
            }
        });
        client.dispatch("vm://in4","some message",null);
        if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS))
        {
            fail("message should have been delivered at least 5 times");
        }
        assertThat(deliveredTimes.intValue(),is(EXPECTED_DELIVERED_TIMES));
    }

    @Test
    public void testRollbackExceptionStrategyCatchMessageRedeliveryDespiteChoiceConfiguration() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.dispatch("vm://in7","some message",null);
        if (!CallMessageProcessor.latch.await(TIMEOUT, TimeUnit.MILLISECONDS))
        {
            fail("custom message processor wasn't call");
        }
    }

    public static class CallMessageProcessor implements MessageProcessor
    {
        public static Latch latch = new Latch();

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            latch.release();
            return event;
        }
    }

}
