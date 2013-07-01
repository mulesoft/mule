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
import org.mule.api.streaming.StreamingDelegate;
import org.mule.api.streaming.Producer;

import java.util.List;

public class PagingDelegateProducer<T> implements Producer<T>
{

    private StreamingDelegate<T> delegate;

    public PagingDelegateProducer(StreamingDelegate<T> delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public List<T> produce()
    {
        return this.delegate.getPage();
    }

    @Override
    public void close() throws MuleException
    {
        this.delegate.close();
    }
}
