/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.common.security.oauth.exception.NotAuthorizedException;
import org.mule.devkit.processor.DevkitBasedMessageProcessor;
import org.mule.security.oauth.OAuthAdapter;
import org.mule.security.oauth.OnNoTokenPolicy;
import org.mule.tck.size.SmallTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DevkitBasedProcessorTestCase
{

    private DevkitBasedMessageProcessor processor;

    @Mock
    private Object testStrategy;

    @Mock
    private OAuthAdapter adapter;

    private MuleEvent event;

    @Before
    public void setUp()
    {
        this.processor = new TestProcessor();
        this.processor.setModuleObject(this.adapter);
        this.event = Mockito.mock(MuleEvent.class);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = MessagingException.class)
    public void unauthorizedException() throws Exception
    {
        Mockito.when(this.testStrategy.toString()).thenThrow(NotAuthorizedException.class);
        Mockito.when(this.adapter.getOnNoTokenPolicy()).thenReturn(OnNoTokenPolicy.EXCEPTION);
        try
        {
            this.processor.process(this.event);
        }
        catch (MessagingException e)
        {
            Assert.assertTrue(e.getCause() instanceof NotAuthorizedException);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void unauthorizedStopChain() throws Exception
    {
        Mockito.when(this.testStrategy.toString()).thenThrow(NotAuthorizedException.class);
        Mockito.when(this.adapter.getOnNoTokenPolicy()).thenReturn(OnNoTokenPolicy.STOP_FLOW);
        Assert.assertNull(this.processor.process(this.event));
    }

    private class TestProcessor extends DevkitBasedMessageProcessor implements MessageProcessor
    {

        private TestProcessor()
        {
            super("test");
        }

        @Override
        protected MuleEvent doProcess(MuleEvent event) throws Exception
        {
            testStrategy.toString();
            return event;
        }

    }
}
