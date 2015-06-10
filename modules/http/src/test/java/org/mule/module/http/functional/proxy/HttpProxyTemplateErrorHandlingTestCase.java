/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.proxy;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.config.MuleProperties;
import org.mule.config.spring.util.ProcessingStrategyUtils;
import org.mule.module.http.functional.requester.AbstractHttpRequestTestCase;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpProxyTemplateErrorHandlingTestCase extends AbstractHttpRequestTestCase
{

    public static String SERVICE_DOWN_MESSAGE = "Service Down";
    public static String CATCH_SENSING_PROCESSOR_NAME = "catchSensingMessageProcessor";
    public static String ROLLBACK_SENSING_PROCESSOR_NAME = "rollbackSensingMessageProcessor";

    @Rule
    public DynamicPort proxyPort = new DynamicPort("proxyPort");

    @Rule
    public SystemProperty systemProperty;

    private String configFile;
    private boolean nonBlocking;


    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {{"http-proxy-template-error-handling-config.xml", false},
                {"http-proxy-template-error-handling-config.xml", true}});
    }

    public HttpProxyTemplateErrorHandlingTestCase(String configFile, boolean nonBlocking)
    {
        this.configFile = configFile;
        this.nonBlocking = nonBlocking;
        if (nonBlocking)
        {
            systemProperty = new SystemProperty(MuleProperties.MULE_DEFAULT_PROCESSING_STRATEGY,
                                                ProcessingStrategyUtils.NON_BLOCKING_PROCESSING_STRATEGY);
        }
    }

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    @Before
    public void startServer() throws Exception
    {
        // Don't start server so that requests fail
    }

    @After
    public void stopServer() throws Exception
    {
        // No server to stop
    }

    @Test
    public void noExceptionStrategy() throws Exception
    {
        HttpResponse response = Request.Get(getProxyUrl("noExceptionStrategy")).connectTimeout(RECEIVE_TIMEOUT)
                .execute().returnResponse();

        assertThat(response.getStatusLine().getStatusCode(), is(500));
    }

    @Test
    public void catchExceptionStrategy() throws Exception
    {
        HttpResponse response = Request.Get(getProxyUrl("catchExceptionStrategy")).connectTimeout(RECEIVE_TIMEOUT)
                .execute().returnResponse();

        assertThat(response.getStatusLine().getStatusCode(), is(200));
        assertThat(IOUtils.toString(response.getEntity().getContent()), equalTo(SERVICE_DOWN_MESSAGE));

        SensingNullMessageProcessor processor = muleContext.getRegistry().lookupObject(CATCH_SENSING_PROCESSOR_NAME);
        assertThat(processor.event, is(notNullValue()));
    }

    @Test
    public void rollbackExceptionStrategy() throws Exception
    {
        HttpResponse response = Request.Get(getProxyUrl("rollbackExceptionStrategy")).connectTimeout(RECEIVE_TIMEOUT)
                .execute().returnResponse();

        assertThat(response.getStatusLine().getStatusCode(), is(500));
        assertThat(IOUtils.toString(response.getEntity().getContent()), not(equalTo(SERVICE_DOWN_MESSAGE)));

        SensingNullMessageProcessor processor = muleContext.getRegistry().lookupObject(ROLLBACK_SENSING_PROCESSOR_NAME);
        assertThat(processor.event, is(notNullValue()));
    }

    private String getProxyUrl(String path)
    {
        return String.format("http://localhost:%s/%s", proxyPort.getNumber(), path);
    }

}
