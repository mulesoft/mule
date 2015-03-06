/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.devkit.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.common.security.oauth.exception.NotAuthorizedException;
import org.mule.security.oauth.OnNoTokenPolicy;
import org.mule.security.oauth.OnNoTokenPolicyAware;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DevkitBasedMessageProcessorTest extends AbstractMuleTestCase
{

    @Test
    public void handlesNotAuthorizedExceptionWithFixedModule() throws Exception
    {
        OnNoTokenPolicyAware policyAware = mock(OnNoTokenPolicyAware.class);
        when(policyAware.getOnNoTokenPolicy()).thenReturn(OnNoTokenPolicy.STOP_FLOW);

        DevkitBasedMessageProcessor processor = new UnauthorizedProcessor("forbidden");
        processor.setModuleObject(policyAware);

        MuleEvent event = mock(MuleEvent.class, RETURNS_DEEP_STUBS);
        MuleEvent responseEvent = processor.process(event);

        assertThat("event should be filtered because STOP_FLOW was used", responseEvent, is(nullValue()));
    }

    @Test
    public void handlesNotAuthorizedExceptionWithExpressionModule() throws Exception
    {
        final String expression = "#[policyAware]";

        OnNoTokenPolicyAware policyAware = mock(OnNoTokenPolicyAware.class);
        when(policyAware.getOnNoTokenPolicy()).thenReturn(OnNoTokenPolicy.STOP_FLOW);

        MuleEvent event = mock(MuleEvent.class, RETURNS_DEEP_STUBS);
        MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
        when(muleContext.getExpressionManager().evaluate(expression, event, true)).thenReturn(policyAware);

        DevkitBasedMessageProcessor processor = new UnauthorizedProcessor("forbidden");
        processor.setMuleContext(muleContext);
        processor.setModuleObject(expression);

        MuleEvent responseEvent = processor.process(event);

        assertThat("event should be filtered because STOP_FLOW was used", responseEvent, is(nullValue()));
    }

    @Test
    public void testThatProcessMethodCanBeOverridden() throws MuleException
    {
        DevkitMessageProcessorProxy connectorMessageProcessor = new DevkitMessageProcessorProxy("anyName");

        connectorMessageProcessor.process(null);

        assertTrue(connectorMessageProcessor.wasCalled);
    }

    private class UnauthorizedProcessor extends DevkitBasedMessageProcessor {

        public UnauthorizedProcessor(String operationName)
        {
            super(operationName);
        }

        @Override
        protected MuleEvent doProcess(MuleEvent event) throws Exception
        {
            throw new NotAuthorizedException("unauthorized");
        }
    }

    private static class DevkitMessageProcessorProxy extends DevkitBasedMessageProcessor
    {

        boolean wasCalled;

        public DevkitMessageProcessorProxy(String operationName)
        {
            super(operationName);
        }

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            wasCalled = true;
            return event;
        }

        @Override
        protected MuleEvent doProcess(MuleEvent event) throws Exception
        {
            wasCalled = false;
            return event;
        }


    }
}
