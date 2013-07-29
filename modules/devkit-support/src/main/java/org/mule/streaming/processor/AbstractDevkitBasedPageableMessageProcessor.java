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

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.streaming.Consumer;
import org.mule.api.streaming.PagingConfiguration;
import org.mule.api.streaming.PagingDelegate;
import org.mule.api.streaming.Producer;
import org.mule.api.streaming.StreamingOutputStrategy;
import org.mule.security.oauth.processor.AbstractDevkitBasedMessageProcessor;
import org.mule.streaming.ConsumerIterator;
import org.mule.streaming.ElementBasedPagingConsumer;
import org.mule.streaming.PagedBasedPagingConsumer;
import org.mule.streaming.PagingDelegateProducer;

public abstract class AbstractDevkitBasedPageableMessageProcessor extends AbstractDevkitBasedMessageProcessor
{

    private StreamingOutputStrategy outputStrategy = StreamingOutputStrategy.ELEMENT;
    private boolean paging;
    private int pageSize;

    public AbstractDevkitBasedPageableMessageProcessor(String operationName)
    {
        super(operationName);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected final MuleEvent doProcess(MuleEvent event) throws Exception
    {
        
        PagingDelegate<?> delegate = this.getPagingDelegate(event, new PagingConfiguration(this.isPaging(), this.getPageSize()));
        Producer<?> producer = new PagingDelegateProducer(delegate);
        Consumer<?> consumer = null;

        if (this.outputStrategy == StreamingOutputStrategy.ELEMENT)
        {
            consumer = new ElementBasedPagingConsumer(producer);
        }
        else if (this.outputStrategy == StreamingOutputStrategy.PAGE)
        {
            consumer = new PagedBasedPagingConsumer(producer);
        }
        else
        {
            throw new DefaultMuleException("Unsupported outputStrategy " + this.outputStrategy);
        }

        event.getMessage().setPayload(new ConsumerIterator(consumer));

        return event;
    }

    protected abstract PagingDelegate<?> getPagingDelegate(MuleEvent event, PagingConfiguration pagingConfiguration) throws Exception;

    public void setOutputStrategy(StreamingOutputStrategy outputStrategy)
    {
        this.outputStrategy = outputStrategy;
    }

    public boolean isPaging()
    {
        return paging;
    }

    public void setPaging(boolean paging)
    {
        this.paging = paging;
    }

    public int getPageSize()
    {
        return pageSize;
    }

    public void setPageSize(int pageSize)
    {
        this.pageSize = pageSize;
    }

    public StreamingOutputStrategy getOutputStrategy()
    {
        return outputStrategy;
    }

}
