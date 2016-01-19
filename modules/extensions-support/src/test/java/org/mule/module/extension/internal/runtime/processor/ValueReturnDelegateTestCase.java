/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.api.metadata.DataType.STRING_DATA_TYPE;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.metadata.DataType;
import org.mule.api.temporary.MuleMessage;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ValueReturnDelegateTestCase extends AbstractMuleTestCase
{

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleContext muleContext;

    @Mock
    private OperationContextAdapter operationContext;

    @Mock
    private MuleEvent event;

    @Mock
    private Serializable attributes;

    private ReturnDelegate delegate;

    @Before
    public void before()
    {
        delegate = new ValueReturnDelegate(muleContext);
        when(operationContext.getEvent()).thenReturn(event);
        when(event.getMessage()).thenReturn(new DefaultMuleMessage("", STRING_DATA_TYPE, attributes));
    }

    @Test
    public void returnsValue()
    {
        Object value = new Object();
        MuleEvent event = delegate.asReturnValue(value, operationContext);
        assertThat(event.getMessage().getPayload(), is(sameInstance(value)));
    }

    @Test
    public void operationReturnsMuleMessageButKeepsAttributes() throws Exception
    {
        Object payload = new Object();
        DataType dataType = mock(DataType.class);

        MuleEvent event = delegate.asReturnValue(new DefaultMuleMessage(payload, dataType), operationContext);

        ArgumentCaptor<DefaultMuleMessage> captor = ArgumentCaptor.forClass(DefaultMuleMessage.class);

        verify(event).setMessage(captor.capture());
        MuleMessage message = captor.getValue();
        assertThat(message, is(notNullValue()));

        assertThat(message.getPayload(), is(sameInstance(payload)));
        assertThat(message.getAttributes(), is(sameInstance(attributes)));
        assertThat(message.getDataType(), is(sameInstance(dataType)));
    }

    @Test
    public void operationReturnsMuleMessageThatOnlySpecifiesPayload() throws Exception
    {
        Object payload = "hello world!";

        MuleEvent event = delegate.asReturnValue(new DefaultMuleMessage(payload), operationContext);

        ArgumentCaptor<DefaultMuleMessage> captor = ArgumentCaptor.forClass(DefaultMuleMessage.class);
        verify(event).setMessage(captor.capture());

        MuleMessage message = captor.getValue();
        assertThat(message, is(notNullValue()));

        assertThat(message.getPayload(), is(sameInstance(payload)));
        assertThat(message.getAttributes(), is(sameInstance(attributes)));
        assertThat(message.getDataType().getType().equals(String.class), is(true));
    }

    @Test
    public void operationReturnsMuleMessageThatOnlySpecifiesPayloadAndAttributes() throws Exception
    {
        Object payload = "hello world!";
        Serializable newAttributes = mock(Serializable.class);

        MuleEvent event = delegate.asReturnValue(new DefaultMuleMessage(payload, newAttributes), operationContext);

        ArgumentCaptor<DefaultMuleMessage> captor = ArgumentCaptor.forClass(DefaultMuleMessage.class);
        verify(event).setMessage(captor.capture());

        MuleMessage message = captor.getValue();
        assertThat(message, is(notNullValue()));

        assertThat(message.getPayload(), is(sameInstance(payload)));
        assertThat(message.getAttributes(), is(sameInstance(newAttributes)));
        assertThat(message.getDataType().getType().equals(String.class), is(true));
    }
}
