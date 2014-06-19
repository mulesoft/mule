/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.streaming.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.devkit.ProcessTemplate;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.security.oauth.callback.ProcessCallback;
import org.mule.streaming.ProviderAwarePagingDelegate;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ManagedPagingDelegateAdapterTestCase
{

    @Mock
    private ProviderAwarePagingDelegate<String, Object> pagingDelegate;

    @Mock
    private MuleEvent event;

    @Mock
    private MessageProcessor messageProcessor;

    private ProcessTemplate<Object, Object> template;

    private List<String> page = Arrays.asList("hello", "world");
    private List<Class<? extends Exception>> managedExceptions;
    private Object provider = new Object();
    private ManagedPagingDelegateAdapter<String> managedPagingDelegate;


    @Before
    public void doSetup() throws Exception
    {
        when(pagingDelegate.getPage(provider)).thenReturn(page);
        when(pagingDelegate.getTotalResults(provider)).thenReturn(page.size());

        managedExceptions = new ArrayList<Class<? extends Exception>>();
        managedExceptions.add(UnsupportedOperationException.class);


        template = new ProcessTemplate<Object, Object>()
        {
            @Override
            public Object execute(ProcessCallback<Object, Object> callback, MessageProcessor messageProcessor, MuleEvent event) throws Exception
            {
                return callback.process(provider);
            }

            @Override
            public Object execute(ProcessCallback<Object, Object> callback, Filter filter, MuleMessage message) throws Exception
            {
                return callback.process(provider);
            }
        };

        managedPagingDelegate = new ManagedPagingDelegateAdapter<String>(pagingDelegate, template, managedExceptions, true, messageProcessor, event);
    }


    @Test
    public void getPage() throws Exception
    {
        assertSame(page, managedPagingDelegate.getPage());
        verify(pagingDelegate).getPage(provider);
    }

    @Test
    public void getTotalResults() throws Exception
    {
        assertEquals(page.size(), managedPagingDelegate.getTotalResults());
        verify(pagingDelegate).getTotalResults(provider);
    }
}
