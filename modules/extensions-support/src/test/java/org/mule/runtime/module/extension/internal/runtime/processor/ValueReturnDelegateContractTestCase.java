/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.DataType.STRING_DATA_TYPE;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.temporary.MuleMessage;
import org.mule.runtime.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public abstract class ValueReturnDelegateContractTestCase extends AbstractMuleTestCase
{
    @Mock(answer = RETURNS_DEEP_STUBS)
    protected MuleContext muleContext;

    @Mock
    protected OperationContextAdapter operationContext;

    @Mock
    protected MuleEvent event;

    @Mock
    protected Serializable attributes;

    protected ReturnDelegate delegate;

    @Before
    public void before()
    {
        delegate = createReturnDelegate();
        when(operationContext.getEvent()).thenReturn(event);
        when(event.getMessage()).thenReturn(new DefaultMuleMessage("", STRING_DATA_TYPE, attributes));
    }

    @Test
    public void returnsSingleValue()
    {
        byte[] value = new byte[] {};
        delegate.asReturnValue(value, operationContext);

        MuleMessage message = getOutputMessage();

        assertThat(message.getPayload(), is(sameInstance(value)));
        assertThat(message.getDataType().getType().equals(byte[].class), is(true));
    }

    @Test
    public void operationReturnsMuleMessageButKeepsAttributes() throws Exception
    {
        Object payload = new Object();
        DataType dataType = mock(DataType.class);

        delegate.asReturnValue(new DefaultMuleMessage(payload, dataType), operationContext);

        MuleMessage message = getOutputMessage();

        assertThat(message.getPayload(), is(sameInstance(payload)));
        assertThat(message.getAttributes(), is(sameInstance(attributes)));
        assertThat(message.getDataType(), is(sameInstance(dataType)));
    }

    @Test
    public void operationReturnsMuleMessageThatOnlySpecifiesPayload() throws Exception
    {
        Object payload = "hello world!";

        delegate.asReturnValue(new DefaultMuleMessage(payload), operationContext);

        MuleMessage message = getOutputMessage();

        assertThat(message.getPayload(), is(sameInstance(payload)));
        assertThat(message.getAttributes(), is(sameInstance(attributes)));
        assertThat(message.getDataType().getType().equals(String.class), is(true));
    }

    @Test
    public void operationReturnsMuleMessageThatOnlySpecifiesPayloadAndAttributes() throws Exception
    {
        Object payload = "hello world!";
        Serializable newAttributes = mock(Serializable.class);

        delegate.asReturnValue(new DefaultMuleMessage(payload, newAttributes), operationContext);

        MuleMessage message = getOutputMessage();

        assertThat(message.getPayload(), is(sameInstance(payload)));
        assertThat(message.getAttributes(), is(sameInstance(newAttributes)));
        assertThat(message.getDataType().getType().equals(String.class), is(true));
    }

    protected abstract ReturnDelegate createReturnDelegate();

    protected abstract MuleMessage getOutputMessage();
}
