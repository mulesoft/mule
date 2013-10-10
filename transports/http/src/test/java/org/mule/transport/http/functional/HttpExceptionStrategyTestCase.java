/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import org.hamcrest.core.Is;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.exception.RollbackSourceCallback;
import org.mule.exception.AbstractMessagingExceptionStrategy;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNot;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class HttpExceptionStrategyTestCase extends FunctionalTestCase
{
    private static final int TIMEOUT = 3000;

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "http-exception-strategy-config.xml";
    }

    @Test
    public void testInExceptionDoRollbackHttpSync() throws Exception
    {
        String url = String.format("http://localhost:%d/flowWithoutExceptionStrategySync", port1.getNumber());
        MuleMessage response = muleContext.getClient().send(url, TEST_MESSAGE, null, TIMEOUT);
        assertThat(response, notNullValue());
        assertThat(response.getPayload(), IsNot.not(IsInstanceOf.instanceOf(NullPayload.class)));
        assertThat(response.getPayloadAsString(), not(TEST_MESSAGE));
        assertThat(response.getExceptionPayload(), notNullValue()); //to be fixed
        assertThat(response.getExceptionPayload(), instanceOf(ExceptionPayload.class)); //to be review/fixed
    }

    @Test
    public void testCustomStatusCodeOnExceptionWithCustomExceptionStrategy() throws Exception
    {
        String url = String.format("http://localhost:%d/flowWithtCESAndStatusCode", port1.getNumber());
        MuleMessage response = muleContext.getClient().send(url, TEST_MESSAGE, null, TIMEOUT);
        assertThat(response, notNullValue());
        assertThat(response.<String>getInboundProperty("http.status"), Is.is("403"));
    }

    public static class CustomExceptionStrategy extends AbstractMessagingExceptionStrategy
    {
        public MuleEvent handleException(Exception ex, MuleEvent event)
        {
            event.getMessage().setOutboundProperty("http.status","403");
            return event;
        }
    }
}
