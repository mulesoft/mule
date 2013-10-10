/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.el.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.transformer.types.DataTypeFactory;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class MessageTestCase extends AbstractELTestCase
{
    public MessageTestCase(Variant variant)
    {
        super(variant);
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
        MuleMessage message = Mockito.mock(MuleMessage.class);
        Mockito.when(message.getUniqueId()).thenReturn("1");
        assertEquals("1", evaluate("message.id", message));
        assertFinalProperty("message.id=2", message);
    }

    @Test
    public void rootId() throws Exception
    {
        MuleMessage message = Mockito.mock(MuleMessage.class);
        Mockito.when(message.getMessageRootId()).thenReturn("2");
        assertEquals("2", evaluate("message.rootId", message));
        assertFinalProperty("message.rootId=2", message);
    }

    @Test
    public void correlationId() throws Exception
    {
        MuleMessage message = Mockito.mock(MuleMessage.class);
        Mockito.when(message.getCorrelationId()).thenReturn("3");
        assertEquals("3", evaluate("message.correlationId", message));
        assertFinalProperty("message.correlationId=2", message);
    }

    @Test
    public void correlationSequence() throws Exception
    {
        MuleMessage message = Mockito.mock(MuleMessage.class);
        Mockito.when(message.getCorrelationSequence()).thenReturn(4);
        assertEquals(4, evaluate("message.correlationSequence", message));
        assertFinalProperty("message.correlationSequence=2", message);
    }

    @Test
    public void correlationGroupSize() throws Exception
    {
        MuleMessage message = Mockito.mock(MuleMessage.class);
        Mockito.when(message.getCorrelationGroupSize()).thenReturn(4);
        assertEquals(4, evaluate("message.correlationGroupSize", message));
        assertFinalProperty("message.correlationGroupSize=2", message);
    }

    @Test
    public void replyTo() throws Exception
    {
        MuleMessage message = Mockito.mock(MuleMessage.class);
        Mockito.when(message.getReplyTo()).thenReturn("replyQueue");
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
        MuleMessage message = Mockito.mock(MuleMessage.class);
        Mockito.when(message.getDataType()).thenReturn((DataType) DataTypeFactory.STRING);
        assertEquals(DataTypeFactory.STRING, evaluate("message.dataType", message));
        assertFinalProperty("message.mimType=2", message);
    }

    @Test
    public void payload() throws Exception
    {
        MuleMessage mockMessage = Mockito.mock(MuleMessage.class);
        Object payload = new Object();
        Mockito.when(mockMessage.getPayload()).thenReturn(payload);
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
        MuleMessage mockMessage = Mockito.mock(MuleMessage.class);
        Banana b = new Banana();
        Mockito.when(mockMessage.getPayload(Mockito.any(Class.class))).thenReturn(b);
        assertSame(b, evaluate("message.payloadAs(org.mule.tck.testmodels.fruit.Banana)", mockMessage));
    }

    @Test
    public void payloadAsDataType() throws Exception
    {
        MuleMessage mockMessage = Mockito.mock(MuleMessage.class);
        Banana b = new Banana();
        Mockito.when(mockMessage.getPayload(Mockito.any(DataType.class))).thenReturn(b);
        assertSame(b,
            evaluate("message.payloadAs(org.mule.transformer.types.DataTypeFactory.STRING)", mockMessage));
    }

    @Test
    @Ignore
    public void transformedPayload() throws Exception
    {
        MuleMessage mockMessage = Mockito.mock(MuleMessage.class);
        Banana b = new Banana();
        Mockito.when(mockMessage.getPayload()).thenReturn("a");
        assertEquals(
            "ab",
            evaluate(
                "message.transformedPayload(new org.mule.transformer.simple.StringAppendTransformer('b'))",
                mockMessage));
    }

}
