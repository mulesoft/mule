/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.streaming;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class PagingDelegateProducerTestCase
{

    @Mock
    private PagingDelegate<String> delegate;

    private PagingDelegateProducer<String> producer;

    @Before
    public void setUp()
    {
        this.producer = new PagingDelegateProducer<String>(this.delegate);
    }

    @Test
    public void produce() throws Exception
    {
        List<String> page = new ArrayList<String>();
        Mockito.when(this.delegate.getPage()).thenReturn(page);
        Assert.assertSame(page, this.producer.produce());
    }

    @Test
    public void totalAvailable()
    {
        final int total = 10;
        Mockito.when(this.delegate.getTotalResults()).thenReturn(total);
        Assert.assertEquals(this.producer.size(), total);
    }

    @Test
    public void closeQuietly() throws MuleException
    {
        this.producer.close();
        Mockito.verify(this.delegate).close();
    }

    @Test(expected = MuleException.class)
    public void closeNoisely() throws MuleException
    {
        Mockito.doThrow(new DefaultMuleException(new Exception())).when(this.delegate).close();
        this.producer.close();

    }
}
