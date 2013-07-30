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

import java.util.Iterator;

/**
 * Base class for devkit generated pageable message processors. This processor
 * automatically takes care of obtaining a
 * {@link org.mule.api.streaming.PagingDelegate} and returning a
 * {@link org.mule.streaming.ConsumerIterator.ConsumerIterator} accordingly
 */
public abstract class AbstractDevkitBasedPageableMessageProcessor extends AbstractDevkitBasedMessageProcessor
{

    /**
     * This attribute specifies if the returned {@link Iterator} should return
     * individual elements or whole pages
     */
    private StreamingOutputUnit outputUnit = StreamingOutputUnit.ELEMENT;

    /**
     * The number of elements to obtain on each invocation to the data source
     */
    private int fetchSize;

    /**
     * Zero-based index of the first page to return. This value has to be equals or
     * greater than zero
     */
    private int firstPage;

    /**
     * Zero-based index of the last page to return. -1 means no limit. If not -1,
     * then it cannot be lower than {@link firstPage}
     */
    private int lastPage;

    public AbstractDevkitBasedPageableMessageProcessor(String operationName)
    {
        super(operationName);
    }

    /**
     * This method sets the message payload to an instance of
     * {@link org.mule.streaming.ConsumerIterator.ConsumerIterator} configured
     * accordingly to the configured outputUnit and the
     * {@link org.mule.api.streaming.PagingDelegate} obtained by invoking {@link
     * org.mule.streaming.processor.AbstractDevkitBasedPageableMessageProcessor.
     * getPagingDelegate(MuleEvent, PagingConfiguration)}
     * 
     * @return a {@link MuleEvent}
     * @throws IllegalArgumentException is firstPage is lower than zero or if
     *             lastPage is lower than zero and firstPage
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected final MuleEvent doProcess(MuleEvent event) throws Exception
    {
        PagingDelegate<?> delegate = this.getPagingDelegate(event, this.makeConfiguration());

        if (delegate == null)
        {
            throw new DefaultMuleException("Obtained paging delegate cannot be null");
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

    /**
     * Implement this method to return the
     * {@link org.mule.api.streaming.PagingDelegate} to be used when paging. This
     * method should never return <code>null</code>
     * 
     * @param event the current mule event
     * @param pagingConfiguration paging configuration parameters
     * @return a not null {@link org.mule.api.streaming.PagingDelegate}
     * @throws Exception
     */
    protected abstract PagingDelegate<?> getPagingDelegate(MuleEvent event,
                                                           PagingConfiguration pagingConfiguration)
        throws Exception;

    private PagingConfiguration makeConfiguration()
    {
        if (this.firstPage < 0)
        {
            throw new IllegalArgumentException("First page index cannot be lower than zero");
        }

        if (this.lastPage > -1 && this.lastPage < this.firstPage)
        {
            throw new IllegalArgumentException("Last page index cannot be lower than first page index");
        }

        return new PagingConfiguration(this.fetchSize, this.firstPage, this.lastPage, this.outputUnit);
    }

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
