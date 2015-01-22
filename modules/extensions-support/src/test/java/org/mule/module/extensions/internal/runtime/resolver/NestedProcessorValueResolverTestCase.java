/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.NestedProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NestedProcessorValueResolverTestCase extends AbstractMuleContextTestCase
{

    private static final String RESPONSE = "Hello world!";

    @Mock
    private MuleContext muleContext;

    @Mock
    private MessageProcessor messageProcessor;

    @Before
    public void before() throws Exception
    {
        when(messageProcessor.process(any(MuleEvent.class))).thenReturn(getTestEvent(RESPONSE));
    }

    @Test
    public void yieldsNestedProcessor() throws Exception
    {
        MuleEvent muleEvent = getTestEvent("");
        NestedProcessorValueResolver resolver = new NestedProcessorValueResolver(messageProcessor);
        NestedProcessor nestedProcessor = resolver.resolve(muleEvent);
        Object response = nestedProcessor.process();
        assertThat((String) response, is(sameInstance(RESPONSE)));

        ArgumentCaptor<MuleEvent> captor = ArgumentCaptor.forClass(MuleEvent.class);
        verify(messageProcessor).process(captor.capture());

        MuleEvent capturedEvent = captor.getValue();
        assertThat(capturedEvent.getId(), is(muleEvent.getId()));
    }

    @Test
    public void alwaysGivesDifferentInstances() throws Exception
    {
        MuleEvent muleEvent = getTestEvent("");
        NestedProcessorValueResolver resolver = new NestedProcessorValueResolver(messageProcessor);
        NestedProcessor resolved1 = resolver.resolve(muleEvent);
        NestedProcessor resolved2 = resolver.resolve(muleEvent);

        assertThat(resolved1, is(not(sameInstance(resolved2))));
    }

}
