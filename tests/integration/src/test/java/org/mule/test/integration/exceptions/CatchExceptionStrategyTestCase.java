/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.ssl.DefaultTlsContextFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class CatchExceptionStrategyTestCase extends AbstractServiceAndFlowTestCase
{
    public static final int TIMEOUT = 5000;
    public static final String ERROR_PROCESSING_NEWS = "error processing news";
    public static final String JSON_RESPONSE = "{\"errorMessage\":\"error processing news\",\"userId\":15,\"title\":\"News title\"}";
    public static final String JSON_REQUEST = "{\"userId\":\"15\"}";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");
    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");
    @Rule
    public DynamicPort dynamicPort3 = new DynamicPort("port3");

    private DefaultTlsContextFactory tlsContextFactory;

    public CatchExceptionStrategyTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{AbstractServiceAndFlowTestCase.ConfigVariant.SERVICE, "org/mule/test/integration/exceptions/catch-exception-strategy-use-case-service.xml"},
                {ConfigVariant.FLOW, "org/mule/test/integration/exceptions/catch-exception-strategy-use-case-flow.xml"}});
    }

    @Before
    public void setup() throws IOException
    {
        tlsContextFactory = new DefaultTlsContextFactory();

        // Configure trust store in the client with the certificate of the server.
        tlsContextFactory.setTrustStorePath("trustStore");
        tlsContextFactory.setTrustStorePassword("mulepassword");
    }

    @Test
    public void testHttpJsonErrorResponse() throws Exception
    {
        testJsonErrorResponse(String.format("http://localhost:%s/service", dynamicPort1.getNumber()));
    }

    @Test
    public void testHttpsJsonErrorResponse() throws Exception
    {
        testJsonErrorResponse(String.format("https://localhost:%s/httpsservice", dynamicPort3.getNumber()));
    }

    @Test
    public void testVmJsonErrorResponse() throws Exception
    {
        testJsonErrorResponse("vm://in");
    }

    @Test
    public void testJmsJsonErrorResponse() throws Exception
    {
        testJsonErrorResponse("jms://in");
    }

    @Test
    public void testTcpJsonErrorResponse() throws Exception
    {
        testJsonErrorResponse(String.format("tcp://localhost:%s", dynamicPort2.getNumber()));
    }

    private void testJsonErrorResponse(String endpointUri) throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        final HttpRequestOptions httpRequestOptions = newOptions().method(POST.name()).tlsContextFactory(tlsContextFactory).responseTimeout(TIMEOUT).build();
        MuleMessage response = client.send(endpointUri, getTestMuleMessage(JSON_REQUEST), httpRequestOptions);
        assertThat(response, IsNull.<Object>notNullValue());
        // compare the structure and values but not the attributes' order
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualJsonNode = mapper.readTree(response.getPayloadAsString());
        JsonNode expectedJsonNode = mapper.readTree(JSON_RESPONSE);
        assertThat(actualJsonNode, Is.is(expectedJsonNode));
    }

    public static final String MESSAGE = "some message";
    public static final String MESSAGE_EXPECTED = "some message consumed successfully";
    
	@Test
	public void testCatchWithComponent() throws Exception
	{
	    LocalMuleClient client = muleContext.getClient();
	    client.dispatch("vm://in2","some message",null);
        MuleMessage result = client.send("vm://in2", MESSAGE, null, TIMEOUT);
        assertThat(result,IsNull.<Object>notNullValue());
        assertThat(result.getPayloadAsString(), Is.is(MESSAGE + " Caught"));
	}

    @Test
    public void testFullyDefinedCatchExceptionStrategyWithComponent() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage result = null;
        result = client.send("vm://in3", MESSAGE, null, TIMEOUT);
        assertThat(result,IsNull.<Object>notNullValue());
        assertThat(result.getPayloadAsString(), Is.is(MESSAGE + " apt1 apt2 groovified"));
    }

    public static class LoadNewsProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            NewsRequest newsRequest = (NewsRequest) event.getMessage().getPayload();
            NewsResponse newsResponse = new NewsResponse();
            newsResponse.setUserId(newsRequest.getUserId());
            newsResponse.setTitle("News title");
            event.getMessage().setPayload(newsResponse);
            return event;
        }
    }

    public static class NewsErrorProcessor implements MessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            ((NewsResponse)event.getMessage().getPayload()).setErrorMessage(ERROR_PROCESSING_NEWS);
            return event;
        }
    }

    public static class NewsRequest
    {
        private int userId;

        public int getUserId()
        {
            return userId;
        }

        public void setUserId(int userId)
        {
            this.userId = userId;
        }
    }

    public static class NewsResponse
    {
        private int userId;
        private String title;
        private String errorMessage;

        public int getUserId()
        {
            return userId;
        }

        public void setUserId(int userId)
        {
            this.userId = userId;
        }

        public String getTitle()
        {
            return title;
        }

        public void setTitle(String title)
        {
            this.title = title;
        }

        public String getErrorMessage()
        {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage)
        {
            this.errorMessage = errorMessage;
        }
    }

    @WebService
    public static class Echo
    {
        @WebResult(name="text")
        public String echo(@WebParam(name="text") String string)
        {
            throw new RuntimeException();
        }
    }

}
