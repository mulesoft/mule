/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain;

import static org.junit.Assert.assertThat;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.DomainFunctionalTestCase;
import org.mule.tck.listener.FlowExecutionListener;

import java.util.Map;

import org.hamcrest.core.Is;
import org.junit.Test;

public class VmInterAppCommunicationTestCase extends DomainFunctionalTestCase
{

    public static final String VM_CLIENT_APP = "vmClientApp";
    public static final String VM_SERVER_APP = "vmServerApp";

    @Override
    protected String getDomainConfig()
    {
        return "domain/vm-shared-connector.xml";
    }

    @Override
    public ApplicationConfig[] getConfigResources()
    {
        return new ApplicationConfig[] {
                new ApplicationConfig(VM_CLIENT_APP, new String[] {"domain/vm-client-app.xml"}),
                new ApplicationConfig(VM_SERVER_APP, new String[] {"domain/vm-server-app.xml"})
        };
    }

    @Test
    public void requestResponse() throws Exception
    {
        //TODO remove reference to AbstractMuleTestCase
        Flow clientFlow = getMuleContextForApp(VM_CLIENT_APP).getRegistry().get("clientFlow");
        MuleEvent response = clientFlow.process(new DefaultMuleEvent(new DefaultMuleMessage("test-data", (Map<String, Object>) null, getMuleContextForApp(VM_CLIENT_APP)), MessageExchangePattern.REQUEST_RESPONSE, clientFlow));
        assertThat(response.getMessageAsString(), Is.is("hello world"));
    }

    @Test
    public void requestReply() throws Exception
    {
        //TODO remove reference to AbstractMuleTestCase
        Flow clientFlow = getMuleContextForApp(VM_CLIENT_APP).getRegistry().get("clientFlowRequestReply");
        MuleEvent response = clientFlow.process(new DefaultMuleEvent(new DefaultMuleMessage("test-data", (Map<String, Object>) null, getMuleContextForApp(VM_CLIENT_APP)), MessageExchangePattern.REQUEST_RESPONSE, clientFlow));
        assertThat(response.getMessageAsString(), Is.is("hello world"));
    }

    @Test
    public void oneWay() throws Exception
    {
        //TODO remove reference to AbstractMuleTestCase
        Flow clientFlow = getMuleContextForApp(VM_CLIENT_APP).getRegistry().get("clientFlowOneWay");
        FlowExecutionListener flowExecutionListener = new FlowExecutionListener(getMuleContextForApp(VM_SERVER_APP));
        MuleEvent response = clientFlow.process(new DefaultMuleEvent(new DefaultMuleMessage("test-data", (Map<String, Object>) null, getMuleContextForApp(VM_CLIENT_APP)), MessageExchangePattern.REQUEST_RESPONSE, clientFlow));
        assertThat(response.getMessageAsString(), Is.is("test-data"));
        flowExecutionListener.waitUntilFlowIsComplete();
    }

}
