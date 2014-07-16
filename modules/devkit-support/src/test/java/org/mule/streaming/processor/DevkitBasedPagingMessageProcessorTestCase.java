/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.streaming.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transformer.TransformerMessagingException;
import org.mule.streaming.ConsumerIterator;
import org.mule.streaming.PagingConfiguration;
import org.mule.streaming.PagingDelegate;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@SmallTest
public class DevkitBasedPagingMessageProcessorTestCase
{

    private static final String PAGE_SIZE = "100";
    private static final int TOP = 1000;

    private MuleEvent event;
    private MuleContext muleContext;

    @Before
    public void setUp()
    {
        this.muleContext = Mockito.mock(MuleContext.class, Mockito.RETURNS_DEEP_STUBS);
        this.event = Mockito.mock(MuleEvent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(this.event.getMuleContext()).thenReturn(this.muleContext);
    }

    @Test
    public void elementBasedIteratior() throws Exception
    {
        TestPagingProcessor processor = this.newProcessor();

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

    @Test(expected = MuleException.class)
    public void nullDelegate() throws Exception
    {
        TestPagingProcessor processor = Mockito.spy(this.newProcessor());
        Mockito.doReturn(null)
            .when(processor)
            .getPagingDelegate(Mockito.any(MuleEvent.class), Mockito.any(PagingConfiguration.class));

        processor.process(event);
    }

    @Test(expected = MessagingException.class)
    public void invalidFetchSize() throws Exception
    {
        TestPagingProcessor processor = this.newProcessor();
        processor.setFetchSize("0");

        processor.process(event);
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
                        for (int i = 0; i < Integer.valueOf(PAGE_SIZE); i++)
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
            Assert.assertEquals(config.getFetchSize(), Integer.valueOf(PAGE_SIZE).intValue());
        }

        @Override
        protected Object evaluateAndTransform(MuleContext muleContext,
                                              MuleEvent event,
                                              Type expectedType,
                                              String expectedMimeType,
                                              Object source)
            throws TransformerException, TransformerMessagingException
        {
            if (Integer.class.equals(expectedType) && source instanceof String)
            {
                return Integer.valueOf((String) source);
            }
            else
            {
                return super.evaluateAndTransform(muleContext, event, expectedType, expectedMimeType, source);
            }
        }
    }

    private TestPagingProcessor newProcessor()
    {
        TestPagingProcessor processor = new TestPagingProcessor();
        processor.setMuleContext(this.muleContext);
        processor.setFetchSize(PAGE_SIZE);

        return processor;
    }
}
