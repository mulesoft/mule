/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.GrapeFruit;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

public class DefaultMessageCollectionTestCase extends AbstractMuleTestCase
{

    Apple apple = new Apple();
    Banana banana = new Banana();
    GrapeFruit grapeFruit = new GrapeFruit();
    Orange orange = new Orange();

    MuleContext muleContext = Mockito.mock(MuleContext.class);

    @Test
    public void addMuleMessage()
    {
        MuleMessage message1 = new DefaultMuleMessage(apple, muleContext);
        MuleMessage message2 = new DefaultMuleMessage(banana, muleContext);
        MuleMessage message3 = new DefaultMuleMessage(orange, muleContext);

        DefaultMessageCollection messageCollection = new DefaultMessageCollection(muleContext);
        messageCollection.addMessage(message1);
        messageCollection.addMessage(message2);
        messageCollection.addMessage(message3);

        assertEquals(3, messageCollection.getMessageList().size());
        assertSame(message1, messageCollection.getMessage(0));
        assertSame(message2, messageCollection.getMessage(1));
        assertSame(message3, messageCollection.getMessage(2));

        assertEquals(3, messageCollection.getPayloadList().size());
        assertTrue(messageCollection.getPayload() instanceof List<?>);
        assertSame(apple, ((List) messageCollection.getPayload()).get(0));
        assertSame(banana, ((List) messageCollection.getPayload()).get(1));
        assertSame(orange, ((List) messageCollection.getPayload()).get(2));
    }

    @Test
    public void addMuleMessageCollection()
    {
        MuleMessageCollection messageCollection1 = Mockito.mock(MuleMessageCollection.class);
        MuleMessageCollection messageCollection2 = Mockito.mock(MuleMessageCollection.class);
        Mockito.when(messageCollection1.getPayload()).thenReturn(new Fruit[]{apple, banana});
        Mockito.when(messageCollection2.getPayload()).thenReturn(new Fruit[]{grapeFruit, orange});

        DefaultMessageCollection messageCollectionUnderTest = new DefaultMessageCollection(muleContext);
        messageCollectionUnderTest.addMessage(messageCollection1);
        messageCollectionUnderTest.addMessage(messageCollection2);

        assertEquals(2, messageCollectionUnderTest.getMessageList().size());
        assertSame(messageCollection1, messageCollectionUnderTest.getMessage(0));
        assertSame(messageCollection2, messageCollectionUnderTest.getMessage(1));

        assertEquals(2, messageCollectionUnderTest.getPayloadList().size());
        assertTrue(messageCollectionUnderTest.getPayload() instanceof List<?>);
        assertSame(apple, ((Fruit[]) ((List) messageCollectionUnderTest.getPayload()).get(0))[0]);
        assertSame(banana, ((Fruit[]) ((List) messageCollectionUnderTest.getPayload()).get(0))[1]);
        assertSame(grapeFruit, ((Fruit[]) ((List) messageCollectionUnderTest.getPayload()).get(1))[0]);
        assertSame(orange, ((Fruit[]) ((List) messageCollectionUnderTest.getPayload()).get(1))[1]);
    }

    @Test
    public void addMuleMessageAndMuleMessageCollection()
    {
        MuleMessage message3 = new DefaultMuleMessage(grapeFruit, muleContext);
        MuleMessage message4 = new DefaultMuleMessage(orange, muleContext);

        MuleMessageCollection messageCollection1 = Mockito.mock(MuleMessageCollection.class);
        Mockito.when(messageCollection1.getPayload()).thenReturn(new Fruit[]{apple, banana});

        DefaultMessageCollection messageCollectionUnderTest = new DefaultMessageCollection(muleContext);
        messageCollectionUnderTest.addMessage(messageCollection1);
        messageCollectionUnderTest.addMessage(message3);
        messageCollectionUnderTest.addMessage(message4);

        assertEquals(3, messageCollectionUnderTest.getMessageList().size());
        assertSame(messageCollection1, messageCollectionUnderTest.getMessage(0));
        assertSame(message3, messageCollectionUnderTest.getMessage(1));
        assertSame(message4, messageCollectionUnderTest.getMessage(2));

        assertEquals(3, messageCollectionUnderTest.getPayloadList().size());
        assertTrue(messageCollectionUnderTest.getPayload() instanceof List<?>);
        assertSame(apple, ((Fruit[]) ((List) messageCollectionUnderTest.getPayload()).get(0))[0]);
        assertSame(banana, ((Fruit[]) ((List) messageCollectionUnderTest.getPayload()).get(0))[1]);
        assertSame(grapeFruit, ((List) messageCollectionUnderTest.getPayload()).get(1));
        assertSame(orange, ((List) messageCollectionUnderTest.getPayload()).get(2));
    }

    @Test
    public void ensureOnlyOneArrayConversionOnCopy()
    {
        DefaultMessageCollection original = Mockito.mock(DefaultMessageCollection.class);
        Mockito.when(original.getMessagesAsArray()).thenReturn(new MuleMessage[]{Mockito.mock(MuleMessage.class)});

        new DefaultMessageCollection(original, muleContext, true);

        Mockito.verify(original, Mockito.times(1)).getMessagesAsArray();
    }

}
