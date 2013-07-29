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
import org.mule.api.streaming.StreamingOutputUnit;
import org.mule.security.oauth.processor.AbstractDevkitBasedMessageProcessor;
import org.mule.streaming.ConsumerIterator;
import org.mule.streaming.ElementBasedPagingConsumer;
import org.mule.streaming.PagedBasedPagingConsumer;
import org.mule.streaming.PagingDelegateProducer;

import java.util.ArrayList;

public abstract class AbstractDevkitBasedPageableMessageProcessor extends AbstractDevkitBasedMessageProcessor
{

    private StreamingOutputUnit outputUnit = StreamingOutputUnit.ELEMENT;
    private int fetchSize;
    private int firstPage;
    private int lastPage;

    public AbstractDevkitBasedPageableMessageProcessor(String operationName)
    {
        super(operationName);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected final MuleEvent doProcess(MuleEvent event) throws Exception
    {

        PagingDelegate<?> delegate = this.getPagingDelegate(event, new PagingConfiguration(this.fetchSize,
            this.firstPage, this.lastPage, this.outputUnit));
        
        if (delegate == null) {
            event.getMessage().setPayload(new ArrayList<Object>().iterator());
            return event;
        }
        
        Producer<?> producer = new PagingDelegateProducer(delegate);
        Consumer<?> consumer = null;

        if (this.outputUnit == StreamingOutputUnit.ELEMENT)
        {
            consumer = new ElementBasedPagingConsumer(producer);
        }
        else if (this.outputUnit == StreamingOutputUnit.PAGE)
        {
            consumer = new PagedBasedPagingConsumer(producer);
        }
        else
        {
            throw new DefaultMuleException("Unsupported outputStrategy " + this.outputUnit);
        }

        event.getMessage().setPayload(new ConsumerIterator(consumer));

        return event;
    }

    protected abstract PagingDelegate<?> getPagingDelegate(MuleEvent event,
                                                           PagingConfiguration pagingConfiguration)
        throws Exception;

    public void setOutputUnit(StreamingOutputUnit outputUnit)
    {
        this.outputUnit = outputUnit;
    }

    public void setFetchSize(int fetchSize)
    {
        this.fetchSize = fetchSize;
    }

    public void setFirstPage(int firstPage)
    {
        this.firstPage = firstPage;
    }

    public void setLastPage(int lastPage)
    {
        this.lastPage = lastPage;
    }

}
