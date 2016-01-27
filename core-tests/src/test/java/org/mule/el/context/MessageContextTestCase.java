/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.context;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.DefaultMuleMessage;
import org.mule.TransformationService;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.metadata.DataType;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;

import org.junit.Before;
import org.junit.Test;

public class MessageContextTestCase extends AbstractELTestCase
{

    private MuleEvent event;
    private MuleMessage message;

    public MessageContextTestCase(Variant variant, String mvelOptimizer)
    {
        super(variant, mvelOptimizer);
    }

    @Before
    public void setup()
    {
        event = mock(MuleEvent.class);
        message = mock(MuleMessage.class);
        doAnswer(invocation -> {
            message = (MuleMessage) invocation.getArguments()[0];
            return null;
        }).when(event).setMessage(any(MuleMessage.class));
        when(event.getMessage()).thenAnswer(invocation -> message);
    }

    @Test
    public void message() throws Exception
    {
        MuleEvent event = getTestEvent("foo");
        assertTrue(evaluate("message", event) instanceof MessageContext);
        assertEquals("foo", evaluate("message.payload", event));
    }

    @Test
    public void assignToMessage() throws Exception
    {
        MuleEvent event = getTestEvent("");
        assertImmutableVariable("message='foo'", event);
    }

    @Test
    public void messageId() throws Exception
    {
        when(message.getUniqueId()).thenReturn("1");
        assertEquals("1", evaluate("message.id", event));
        assertFinalProperty("message.id=2", event);
    }

    @Test
    public void rootId() throws Exception
    {
        when(message.getMessageRootId()).thenReturn("2");
        assertEquals("2", evaluate("message.rootId", event));
        assertFinalProperty("message.rootId=2", event);
    }

    @Test
    public void correlationId() throws Exception
    {
        when(message.getCorrelationId()).thenReturn("3");
        assertEquals("3", evaluate("message.correlationId", event));
        assertFinalProperty("message.correlationId=2", event);
    }

    @Test
    public void correlationSequence() throws Exception
    {
        when(message.getCorrelationSequence()).thenReturn(4);
        assertEquals(4, evaluate("message.correlationSequence", event));
        assertFinalProperty("message.correlationSequence=2", event);
    }

    @Test
    public void correlationGroupSize() throws Exception
    {
        when(message.getCorrelationGroupSize()).thenReturn(4);
        assertEquals(4, evaluate("message.correlationGroupSize", event));
        assertFinalProperty("message.correlationGroupSize=2", event);
    }

    @Test
    public void replyTo() throws Exception
    {
        when(message.getReplyTo()).thenReturn("replyQueue");
        assertEquals("replyQueue", evaluate("message.replyTo", event));
        assertFinalProperty("message.correlationGroupSize=2", event);
    }

    @Test
    public void assignValueToReplyTo() throws Exception
    {
        MuleEvent event = getTestEvent("");
        evaluate("message.replyTo='my://uri'", event);
        assertEquals("my://uri", event.getMessage().getReplyTo());
    }

    @Test
    public void dataType() throws Exception
    {
        when(message.getDataType()).thenReturn((DataType) DataTypeFactory.STRING);
        assertEquals(DataTypeFactory.STRING, evaluate("message.dataType", event));
        assertFinalProperty("message.mimType=2", event);
    }

    @Test
    public void payload() throws Exception
    {
        MuleEvent event = mock(MuleEvent.class);
        MuleMessage message = mock(MuleMessage.class);
        when(event.getMessage()).thenReturn(message);
        Object payload = new Object();
        when(message.getPayload()).thenReturn(payload);
        assertSame(payload, evaluate("message.payload", event));
    }

    @Test
    public void assignPayload() throws Exception
    {
        message = new DefaultMuleMessage("", muleContext);
        evaluate("message.payload = 'foo'", event);
        assertEquals("foo", event.getMessage().getPayload());
    }

    @Test
    public void payloadAsType() throws Exception
    {
        MuleMessage transformedMessage = mock(MuleMessage.class, RETURNS_DEEP_STUBS);
        TransformationService transformationService = mock(TransformationService.class);
        muleContext.setTransformationService(transformationService);
        when(transformationService.transform(any(MuleMessage.class), any(DataType.class))).thenReturn(transformedMessage);
        assertSame(transformedMessage.getPayload(), evaluate("message.payloadAs(org.mule.tck.testmodels.fruit.Banana)", event));
    }

    @Test
    public void payloadAsDataType() throws Exception
    {
        String payload = TEST_PAYLOAD;
        MuleMessage transformedMessage = mock(MuleMessage.class, RETURNS_DEEP_STUBS);
        TransformationService transformationService = mock(TransformationService.class);
        when(transformedMessage.getPayload()).thenReturn(TEST_PAYLOAD);
        muleContext.setTransformationService(transformationService);
        when(transformationService.transform(event.getMessage(), DataTypeFactory.STRING)).thenReturn(transformedMessage);
        Object result = evaluate("message.payloadAs(org.mule.transformer.types.DataTypeFactory.STRING)", event);
        assertSame(TEST_PAYLOAD, result);
    }

    @Test
    public void nullPayloadTest() throws Exception
    {
        when(message.getPayload()).thenReturn(NullPayload.getInstance());
        assertEquals(true, evaluate("message.payload == null", event));
        assertEquals(true, evaluate("payload == null", event));
        assertEquals(false, evaluate("message.payload is NullPayload", event));
        assertEquals(true, evaluate("message.payload == empty", event));
    }

    @Test
    public void attributes() throws Exception
    {
        Banana banana = new Banana();
        when(message.getAttributes()).thenReturn(banana);
        assertThat(evaluate("message.attributes", event), is(banana));
    }
}
