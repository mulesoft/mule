/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.exception.AbstractMessagingExceptionStrategy;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNot;
import org.junit.Rule;
import org.junit.Test;

public class HttpExceptionStrategyTestCase extends FunctionalTestCase
{
    private static final int TIMEOUT = 3000;

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
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
        @Override
        public MuleEvent handleException(Exception ex, MuleEvent event)
        {
            event.getMessage().setOutboundProperty("http.status","403");
            return event;
        }
    }
}
