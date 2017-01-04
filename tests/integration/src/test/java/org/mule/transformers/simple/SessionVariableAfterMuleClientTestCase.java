/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.api.transport.PropertyScope.SESSION;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

public class SessionVariableAfterMuleClientTestCase extends FunctionalTestCase
{

    private static final String SESSION_VARIABLE_VALUE = "TEST";
    private static String inboundSessionVariableValue ;
    private static String outboundSessionVariableValue ;

    @Override
    protected String getConfigFile()
    {
        return "session-variable-after-mule-client.xml";
    }

    @Before
    public void setUp() throws Exception
    {
        inboundSessionVariableValue = null;
        outboundSessionVariableValue = null;
    }

    @Test
    public void testSessionVariableExistsAfterDispatch() throws Exception
    {
        runFlow("flow-dispatcher");
        assertThat(inboundSessionVariableValue, is(SESSION_VARIABLE_VALUE));
        assertThat(outboundSessionVariableValue, is(SESSION_VARIABLE_VALUE));
    }

    @Test
    public void testSessionVariableExistsAfterSend() throws Exception
    {
        runFlow("flow-sender");
        assertThat(inboundSessionVariableValue, is(SESSION_VARIABLE_VALUE));
        assertThat(outboundSessionVariableValue, is(SESSION_VARIABLE_VALUE));
    }

    public static class DispatchJavaComponent implements Callable
    {
        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            inboundSessionVariableValue = eventContext.getMessage().getProperty("testVariable", SESSION);
            eventContext.getMuleContext().getClient().dispatch("vm://in", eventContext.getMessage());
            return eventContext.getMessage().getPayload();
        }
    }

    public static class SendJavaComponent implements Callable
    {
        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            inboundSessionVariableValue = eventContext.getMessage().getProperty("testVariable", SESSION);
            eventContext.getMuleContext().getClient().send("vm://in", eventContext.getMessage());
            return eventContext.getMessage().getPayload();
        }
    }

    public static class OutboundJavaComponent implements Callable
    {
        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            outboundSessionVariableValue = eventContext.getMessage().getProperty("testVariable", SESSION);
            return eventContext.getMessage().getPayload();
        }
    }
}
