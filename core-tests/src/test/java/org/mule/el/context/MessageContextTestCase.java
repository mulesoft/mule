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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.DefaultMuleMessage;
import org.mule.TransformationService;
import org.mule.api.MuleMessage;
import org.mule.api.metadata.DataType;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;

import org.junit.Test;

public class MessageContextTestCase extends AbstractELTestCase
{

    public MessageContextTestCase(Variant variant, String mvelOptimizer)
    {
        super(variant, mvelOptimizer);
    }

    @Test
    public void message() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("foo", muleContext);
        assertTrue(evaluate("message", message) instanceof MessageContext);
        assertEquals("foo", evaluate("message.payload", message));
    }

    @Test
    public void assignToMessage() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertImmutableVariable("message='foo'", message);
    }

    @Test
    public void messageId() throws Exception
    {
        MuleMessage message = mock(MuleMessage.class);
        when(message.getUniqueId()).thenReturn("1");
        assertEquals("1", evaluate("message.id", message));
        assertFinalProperty("message.id=2", message);
    }

    @Test
    public void rootId() throws Exception
    {
        MuleMessage message = mock(MuleMessage.class);
        when(message.getMessageRootId()).thenReturn("2");
        assertEquals("2", evaluate("message.rootId", message));
        assertFinalProperty("message.rootId=2", message);
    }

    @Test
    public void correlationId() throws Exception
    {
        MuleMessage message = mock(MuleMessage.class);
        when(message.getCorrelationId()).thenReturn("3");
        assertEquals("3", evaluate("message.correlationId", message));
        assertFinalProperty("message.correlationId=2", message);
    }

    @Test
    public void correlationSequence() throws Exception
    {
        MuleMessage message = mock(MuleMessage.class);
        when(message.getCorrelationSequence()).thenReturn(4);
        assertEquals(4, evaluate("message.correlationSequence", message));
        assertFinalProperty("message.correlationSequence=2", message);
    }

    @Test
    public void correlationGroupSize() throws Exception
    {
        MuleMessage message = mock(MuleMessage.class);
        when(message.getCorrelationGroupSize()).thenReturn(4);
        assertEquals(4, evaluate("message.correlationGroupSize", message));
        assertFinalProperty("message.correlationGroupSize=2", message);
    }

    @Test
    public void replyTo() throws Exception
    {
        MuleMessage message = mock(MuleMessage.class);
        when(message.getReplyTo()).thenReturn("replyQueue");
        assertEquals("replyQueue", evaluate("message.replyTo", message));
        assertFinalProperty("message.correlationGroupSize=2", message);
    }

    @Test
    public void assignValueToReplyTo() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        evaluate("message.replyTo='my://uri'", message);
        assertEquals("my://uri", message.getReplyTo());
    }

    @Test
    public void dataType() throws Exception
    {
        MuleMessage message = mock(MuleMessage.class);
        when(message.getDataType()).thenReturn((DataType) DataTypeFactory.STRING);
        assertEquals(DataTypeFactory.STRING, evaluate("message.dataType", message));
        assertFinalProperty("message.mimType=2", message);
    }

    @Test
    public void payload() throws Exception
    {
        MuleMessage mockMessage = mock(MuleMessage.class);
        Object payload = new Object();
        when(mockMessage.getPayload()).thenReturn(payload);
        assertSame(payload, evaluate("message.payload", mockMessage));
    }

    @Test
    public void assignPayload() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        evaluate("message.payload = 'foo'", message);
        assertEquals("foo", message.getPayload());
    }

    @Test
    public void payloadAsType() throws Exception
    {
        MuleMessage mockMessage = mock(MuleMessage.class);
        TransformationService transformationService = mock(TransformationService.class);
        muleContext.setTransformationService(transformationService);
        Banana b = new Banana();
        when(transformationService.getPayload(any(MuleMessage.class), any(Class.class))).thenReturn(b);
        assertSame(b, evaluate("message.payloadAs(org.mule.tck.testmodels.fruit.Banana)", mockMessage));
    }

    @Test
    public void payloadAsDataType() throws Exception
    {
        MuleMessage mockMessage = mock(MuleMessage.class);
        TransformationService transformationService = mock(TransformationService.class);
        muleContext.setTransformationService(transformationService);
        Banana b = new Banana();
        when(transformationService.getPayload(any(MuleMessage.class), any(DataType.class))).thenReturn(b);
        assertSame(b,
            evaluate("message.payloadAs(org.mule.transformer.types.DataTypeFactory.STRING)", mockMessage));
    }

    @Test
    public void nullPayloadTest() throws Exception
    {
        MuleMessage mockMessage = mock(MuleMessage.class);
        when(mockMessage.getPayload()).thenReturn(NullPayload.getInstance());
        assertEquals(true, evaluate("message.payload == null", mockMessage));
        assertEquals(true, evaluate("payload == null", mockMessage));
        assertEquals(false, evaluate("message.payload is NullPayload", mockMessage));
        assertEquals(true, evaluate("message.payload == empty", mockMessage));
    }

    @Test
    public void attributes() throws Exception
    {
        MuleMessage mockMessage = mock(MuleMessage.class);
        Banana banana = new Banana();
        when(mockMessage.getAttributes()).thenReturn(banana);
        assertThat(evaluate("message.attributes", mockMessage), is(banana));
    }
}
