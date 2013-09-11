/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.streaming;

import org.mule.api.MuleException;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class ElementBasedPagingConsumerTestCase extends AbstractPagingConsumerTestCase
{

    private Consumer<Integer> consumer;
    private Producer<Integer> producer;

    @Before
    public void setUp()
    {
        this.pages = this.getPages();
        this.producer = Mockito.spy(new TestProducer());
        this.consumer = Mockito.spy(new ElementBasedPagingConsumer<Integer>(this.producer));
    }

    @Test(expected = ClosedConsumerException.class)
    public void happyPath() throws Exception
    {
        List<Integer> elements = new ArrayList<Integer>();
        while (!this.consumer.isConsumed())
        {
            elements.add(this.consumer.consume());
        }

        Assert.assertEquals(elements.size(), totalCount);
        Assert.assertTrue(this.consumer.isConsumed());

        for (List<Integer> page : pages)
        {
            Assert.assertTrue(elements.containsAll(page));
        }

        Mockito.verify(this.consumer).close();
        Mockito.verify(this.producer).close();

        this.consumer.consume();
    }

    @Test(expected = ClosedConsumerException.class)
    public void closeEarly() throws Exception
    {
        List<Integer> elements = new ArrayList<Integer>();

        for (int i = 0; i < pageSize; i++)
        {
            elements.add(this.consumer.consume());
        }

        this.consumer.close();
        Assert.assertEquals(pageSize, elements.size());
        Assert.assertTrue(elements.containsAll(this.pages.get(0)));
        Assert.assertTrue(this.consumer.isConsumed());
        this.consumer.consume();
    }

    @Test
    public void totalAvailable()
    {
        Assert.assertEquals(this.consumer.totalAvailable(), totalCount);
    }

    @Test
    public void doubleClose() throws MuleException
    {
        this.consumer.close();
        this.consumer.close();
    }

    private class TestProducer implements Producer<Integer>
    {

        private int index = 0;

        @Override
        public void close() throws MuleException
        {
        }

        @Override
        public List<Integer> produce()
        {
            List<Integer> ret;

            if (this.index < pages.size())
            {

                ret = pages.get(this.index);
                this.index++;
            }
            else
            {
                ret = new ArrayList<Integer>();
            }

            return ret;
        }

        public int totalAvailable()
        {
            return totalCount;
        };
    }

}
