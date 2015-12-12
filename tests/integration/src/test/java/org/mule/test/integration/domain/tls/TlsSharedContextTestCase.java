/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.tls;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.module.http.api.requester.HttpRequesterConfig;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.DomainFunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.ssl.api.TlsContextFactory;

import org.junit.Rule;
import org.junit.Test;

public class TlsSharedContextTestCase extends DomainFunctionalTestCase
{

    private static final String DATA = "data";
    private static final String FIRST_APP = "firstApp";
    private static final String SECOND_APP = "secondApp";

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");
    @Rule
    public DynamicPort port2 = new DynamicPort("port2");
    @Rule
    public DynamicPort port3 = new DynamicPort("port3");

    @Override
    protected String getDomainConfig()
    {
        return "domain/tls/tls-domain-config.xml";
    }

    @Override
    public ApplicationConfig[] getConfigResources()
    {
        return new ApplicationConfig[] {new ApplicationConfig(FIRST_APP, new String[] {"domain/tls/tls-first-app-config.xml"}),
                new ApplicationConfig(SECOND_APP, new String[] {"domain/tls/tls-second-app-config.xml"})
        };
    }

    @Test
    public void sharedRequesterUsingSharedTlsContextToLocalListener() throws Exception
    {
        testFlowForApp("helloWorldClientFlow", FIRST_APP, "hello world");
    }

    @Test
    public void localRequesterToSharedListenerUsingSharedTlsContext() throws Exception
    {
        testFlowForApp("helloMuleClientFlow", SECOND_APP, "hello mule");
    }

    @Test
    public void muleClientUsingLocalRequesterWithSharedTlsContextToListenerUsingSharedTlsContext() throws Exception
    {
        MuleContext secondAppContext = getMuleContextForApp(SECOND_APP);
        HttpRequesterConfig requesterConfig = secondAppContext.getRegistry().lookupObject("requestConfig");
        HttpRequestOptions requestConfigOptions = HttpRequestOptionsBuilder.newOptions().requestConfig(requesterConfig).build();
        testMuleClient(requestConfigOptions);
    }

    @Test
    public void muleClientUsingSharedTlsContextToListenerUsingSharedTlsContext() throws Exception
    {
        MuleContext domainContext = getMuleContextForDomain();
        TlsContextFactory tlsContextFactory = domainContext.getRegistry().lookupObject("sharedTlsContext2");
        HttpRequestOptions tlsContextOptions = HttpRequestOptionsBuilder.newOptions().tlsContextFactory(tlsContextFactory).build();
        testMuleClient(tlsContextOptions);
    }

    private void testMuleClient(HttpRequestOptions operationOptions) throws Exception
    {
        MuleContext context = getMuleContextForApp(SECOND_APP);
        MuleMessage response = context.getClient().send(String.format("https://localhost:%s/helloAll", port3.getValue()), new DefaultMuleMessage(DATA, context), operationOptions);
        assertThat(response.getPayloadAsString(), is("hello all"));
    }

    private void testFlowForApp(String flowName, String appName, String expected) throws Exception
    {
        MuleContext appContext = getMuleContextForApp(appName);
        Flow helloWorldFlow = (Flow) appContext.getRegistry().lookupFlowConstruct(flowName);
        MuleEvent response = helloWorldFlow.process(MuleTestUtils.getTestEvent(DATA, MessageExchangePattern.REQUEST_RESPONSE, appContext));
        assertThat(response.getMessage().getPayloadAsString(), is(expected));
    }


}
