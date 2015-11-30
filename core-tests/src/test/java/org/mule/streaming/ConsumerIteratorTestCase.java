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

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;

@SmallTest
public class ConsumerIteratorTestCase
{

    private static final int PAGE_SIZE = 100;
    private static final int TOP = 3000;

    private PagingDelegate<String> delegate = new PagingDelegate<String>()
    {

        long counter = 0;

        public List<String> getPage()
        {
            if (counter < TOP)
            {
                List<String> page = new ArrayList<String>(100);
                for (int i = 0; i < PAGE_SIZE; i++)
                {
                    counter++;
                    String value = RandomStringUtils.randomAlphabetic(5000);
                    page.add(value);
                }

                return page;
            }

            return null;
        };

        public void close() throws MuleException
        {
        };

        @Override
        public int getTotalResults()
        {
            return TOP;
        }
    };

    @Test
    public void iterateStreaming() throws Exception
    {
        ConsumerIterator<String> it = this.newIterator();

        int count = 0;
        while (it.hasNext())
        {
            it.next();
            count++;
        }

        Assert.assertEquals(count, TOP);
        it.close();
    }

    @Test
    public void closedIterator() throws Exception
    {
        ConsumerIterator<String> it = this.newIterator();
        it.close();
        Assert.assertFalse(it.hasNext());
    }

    @Test
    public void closedConsumer() throws Exception
    {
        Producer<List<String>> producer = new PagingDelegateProducer<String>(this.delegate);
        Consumer<String> consumer = new ListConsumer<String>(producer);

        ConsumerIterator<String> it = new ConsumerIterator<String>(consumer);

        consumer.close();
        Assert.assertFalse(it.hasNext());
    }

    @Test
    public void size() throws Exception
    {
        ConsumerIterator<String> it = this.newIterator();
        Assert.assertEquals(it.size(), TOP);
    }

    private ConsumerIterator<String> newIterator()
    {
        Producer<List<String>> producer = new PagingDelegateProducer<String>(this.delegate);
        Consumer<String> consumer = new ListConsumer<String>(producer);

        ConsumerIterator<String> it = new ConsumerIterator<String>(consumer);
        return it;
    }

}
