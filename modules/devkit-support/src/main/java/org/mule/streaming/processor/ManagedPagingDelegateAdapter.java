/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.streaming.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.devkit.ProcessTemplate;
import org.mule.api.processor.MessageProcessor;
import org.mule.security.oauth.callback.ProcessCallback;
import org.mule.streaming.PagingDelegate;
import org.mule.streaming.ProviderAwarePagingDelegate;

import java.util.List;

/**
 * Adapter class to adapt a {@link org.mule.streaming.ProviderAwarePagingDelegate} into
 * a {@link org.mule.streaming.PagingDelegate}
 *
 * Allows executing paging operations in a managed context defined by an owning {@link org.mule.api.devkit.ProcessTemplate}
 */
public class ManagedPagingDelegateAdapter<T> extends PagingDelegate<T>
{

    private final ProcessTemplate<Object, Object> processTemplate;
    private final List<Class<? extends Exception>> managedExceptions;
    private final boolean isProtected;
    private final MessageProcessor originalMessageProcessor;
    private final MuleEvent originalEvent;
    private final ProviderAwarePagingDelegate<T, Object> delegate;

    public ManagedPagingDelegateAdapter(ProviderAwarePagingDelegate<T, Object> delegate,
                                        ProcessTemplate<Object, Object> processTemplate,
                                        List<Class<? extends Exception>> managedExceptions,
                                        boolean isProtected,
                                        final MessageProcessor originalMessageProcessor,
                                        final MuleEvent originalEvent)
    {
        this.delegate = delegate;
        this.processTemplate = processTemplate;
        this.managedExceptions = managedExceptions;
        this.isProtected = isProtected;
        this.originalMessageProcessor = originalMessageProcessor;
        this.originalEvent = originalEvent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> getPage()
    {
        try
        {
            return (List<T>) processTemplate.execute(new ProcessCallback<Object, Object>()
            {
                @Override
                @SuppressWarnings("unchecked")
                public Object process(Object object) throws Exception
                {
                    return delegate.getPage(object);
                }

                @Override
                public List<Class<? extends Exception>> getManagedExceptions()
                {
                    return managedExceptions;
                }

                @Override
                public boolean isProtected()
                {
                    return isProtected;
                }
            }, originalMessageProcessor, originalEvent);

        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    @Override
    public int getTotalResults()
    {
        try
        {
            return (Integer) processTemplate.execute(new ProcessCallback<Object, Object>()
            {
                @Override
                @SuppressWarnings("unchecked")
                public Object process(Object object) throws Exception
                {
                    return delegate.getTotalResults(object);
                }

                @Override
                public List<Class<? extends Exception>> getManagedExceptions()
                {
                    return managedExceptions;
                }

                @Override
                public boolean isProtected()
                {
                    return isProtected;
                }
            }, originalMessageProcessor, originalEvent);

        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    @Override
    public void close() throws MuleException
    {
        delegate.close();
    }
}
