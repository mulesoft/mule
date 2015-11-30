/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.streaming;

import org.mule.tck.size.SmallTest;
import org.mule.util.queue.Queue;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class QueueProducerTestCase
{
    @Mock
    private Queue queue;;

    private Set<String> values;
    private Iterator<String> valuesIterator;
    private QueueProducer<Serializable> producer;

    @Before
    public void setUp() throws Exception
    {
        this.values = new HashSet<String>();
        this.values.add("banana");
        this.values.add("chocolate");
        this.values.add("coke");

        this.valuesIterator = this.values.iterator();

        Mockito.when(this.queue.poll(Mockito.anyLong())).thenAnswer(new Answer<Serializable>()
        {
            @Override
            public Serializable answer(InvocationOnMock invocation) throws Throwable
            {
                return valuesIterator.hasNext() ? valuesIterator.next() : null;
            }
        });

        Mockito.when(this.queue.size()).thenReturn(this.values.size());

        this.producer = new QueueProducer<Serializable>(this.queue);
    }

    @Test
    public void happyPath() throws Exception
    {
        Set<Serializable> returnedValues = new HashSet<Serializable>();

        Serializable page = this.producer.produce();
        while (page != null)
        {
            returnedValues.add(page);
            page = this.producer.produce();
        }

        Assert.assertEquals(returnedValues.size(), this.values.size());

        for (String value : this.values)
        {
            Assert.assertTrue(returnedValues.contains(value));
        }

        Assert.assertNull(this.producer.produce());
    }

    @Test
    public void size() throws Exception
    {
        Assert.assertEquals(this.values.size(), this.producer.size());
    }

    @Test
    public void earlyClose() throws Exception
    {
        this.producer.produce();
        this.producer.close();

        Assert.assertNull(this.producer.produce());
    }

}
