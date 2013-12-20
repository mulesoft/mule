/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.streaming;

import org.mule.api.Closeable;

import java.util.List;

/**
 * A PagingDelegate is a {@link Closeable} capable of consuming a data feed in pages.
 * Implementing this class does not guarantee thread safeness. Check each particular
 * implementation for information about that
 * 
 * @since 3.5.0
 */
public abstract class PagingDelegate<T> implements Closeable
{

    /**
     * Returns the next page of items. If the return value is <code>null</code> or an
     * empty list, then it means no more items are available
     * 
     * @return a populated list of elements. <code>null</code> or an empty list, then
     *         it means no more items are available
     */
    public abstract List<T> getPage();

    /**
     * returns the total amount of items in the unpaged resultset. In some scenarios,
     * it might not be possible/convenient to actually retrieve this value. -1 is
     * returned in such a case.
     */
    public abstract int getTotalResults();

}
