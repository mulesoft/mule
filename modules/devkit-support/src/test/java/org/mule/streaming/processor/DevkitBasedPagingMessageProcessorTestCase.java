/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.streaming.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.streaming.PagingConfiguration;
import org.mule.api.streaming.PagingDelegate;
import org.mule.api.streaming.StreamingOutputUnit;
import org.mule.streaming.ConsumerIterator;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@SmallTest
public class DevkitBasedPagingMessageProcessorTestCase
{

    private static final int PAGE_SIZE = 100;
    private static final int TOP = 1000;
    private static final int FIRST_PAGE = 0;
    private static final int LAST_PAGE = 10;

    @Test
    public void elementBasedIteratior() throws Exception
    {
        TestPagingProcessor processor = this.newProcessor();
        processor.setOutputUnit(StreamingOutputUnit.ELEMENT);

        MuleEvent event = Mockito.mock(MuleEvent.class, Mockito.RETURNS_DEEP_STUBS);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<ConsumerIterator> captor = ArgumentCaptor.forClass(ConsumerIterator.class);

        processor.process(event);

        Mockito.verify(event.getMessage()).setPayload(captor.capture());

        @SuppressWarnings("unchecked")
        ConsumerIterator<String> it = (ConsumerIterator<String>) captor.getValue();

        for (int i = 0; i < TOP; i++)
        {
            Assert.assertTrue(it.hasNext());
            Assert.assertNotNull(it.next());
        }

        Assert.assertFalse(it.hasNext());
    }

    @Test
    public void pagedBasedIterator() throws Exception
    {
        TestPagingProcessor processor = this.newProcessor();
        processor.setOutputUnit(StreamingOutputUnit.PAGE);

        MuleEvent event = Mockito.mock(MuleEvent.class, Mockito.RETURNS_DEEP_STUBS);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<ConsumerIterator> captor = ArgumentCaptor.forClass(ConsumerIterator.class);

        processor.process(event);

        Mockito.verify(event.getMessage()).setPayload(captor.capture());

        @SuppressWarnings("unchecked")
        ConsumerIterator<List<String>> it = (ConsumerIterator<List<String>>) captor.getValue();

        for (int i = 0; i < TOP / PAGE_SIZE; i++)
        {
            Assert.assertTrue(it.hasNext());
            List<String> page = it.next();
            Assert.assertTrue(page != null && page.size() == PAGE_SIZE);
        }

        Assert.assertFalse(it.hasNext());
    }

    @Test(expected = MuleException.class)
    public void nullPagingStrategy() throws Exception
    {
        TestPagingProcessor processor = this.newProcessor();
        processor.setOutputUnit(null);

        MuleEvent event = Mockito.mock(MuleEvent.class, Mockito.RETURNS_DEEP_STUBS);
        processor.process(event);
    }
    
    @Test
    public void nullDelegate() throws Exception
    {
        TestPagingProcessor processor = Mockito.spy(this.newProcessor());
        Mockito.doReturn(null).when(processor).getPagingDelegate(Mockito.any(MuleEvent.class), Mockito.any(PagingConfiguration.class));
        
        MuleEvent event = Mockito.mock(MuleEvent.class, Mockito.RETURNS_DEEP_STUBS);
        
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Iterator> captor = ArgumentCaptor.forClass(Iterator.class);

        processor.process(event);
        
        Mockito.verify(event.getMessage()).setPayload(captor.capture());
        
        Iterator<?> it = (Iterator<?>) captor.getValue();
        Assert.assertFalse(it.hasNext());
    }

    private class TestPagingProcessor extends AbstractDevkitBasedPageableMessageProcessor
    {

        private TestPagingProcessor()
        {
            super("paging-test");
        }

        @Override
        protected PagingDelegate<?> getPagingDelegate(MuleEvent event, PagingConfiguration pagingConfiguration)
        {

            this.assertPagingConfiguration(pagingConfiguration);

            // This delegate doesn't care about the pagingConfiguration because what
            // I want to test is the MP not the
            // actual behaviour of the delegate
            return new PagingDelegate<String>()
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
        }

        private void assertPagingConfiguration(PagingConfiguration config)
        {
            Assert.assertEquals(config.getLastPage(), LAST_PAGE);
            Assert.assertEquals(config.getFirstPage(), FIRST_PAGE);
            Assert.assertEquals(config.getFetchSize(), PAGE_SIZE);
        }
    }
    
    private TestPagingProcessor newProcessor()
    {
        TestPagingProcessor processor = new TestPagingProcessor();
        processor.setOutputUnit(StreamingOutputUnit.ELEMENT);
        processor.setFetchSize(PAGE_SIZE);
        processor.setFirstPage(FIRST_PAGE);
        processor.setLastPage(LAST_PAGE);

        return processor;
    }
}
