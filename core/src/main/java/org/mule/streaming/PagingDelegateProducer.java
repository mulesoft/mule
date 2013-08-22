/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.streaming;

import org.mule.api.MuleException;

import java.util.List;

/**
 * Implementation of {@link org.mule.streaming.Producer} that uses an instance of
 * {@link org.mule.streaming.PagingDelegate} to get its results
 */
public class PagingDelegateProducer<T> implements Producer<T>
{

    private PagingDelegate<T> delegate;

    public PagingDelegateProducer(PagingDelegate<T> delegate)
    {
        this.delegate = delegate;
    }

    /**
     * Asks the delegate for the next page
     */
    @Override
    public List<T> produce()
    {
        return this.delegate.getPage();
    }

    /**
     * Returns the total amount of available results informed by delegate
     */
    @Override
    public int totalAvailable()
    {
        return this.delegate.getTotalResults();
    }

    /**
     * Closes the delegate
     */
    @Override
    public void close() throws MuleException
    {
        this.delegate.close();
    }
}
