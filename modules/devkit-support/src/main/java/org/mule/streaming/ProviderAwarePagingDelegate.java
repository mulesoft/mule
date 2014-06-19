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
 * Similar to {@link org.mule.streaming.PagingDelegate} but aware of the actual
 * component that provides the data being paged
 *
 * @param <T> the type of the data to be returned
 * @param <P> the type of the provider generating the data
 */
public abstract class ProviderAwarePagingDelegate<T, P> implements Closeable
{

    /**
     * Returns the next page of items. If the return value is <code>null</code> or an
     * empty list, then it means no more items are available
     *
     * @param provider The provider to be used to do the query. You can assume this provider is already properly initialised
     * @return a populated list of elements. <code>null</code> or an empty list, then
     * it means no more items are available
     * @throws Exception
     */
    public abstract List<T> getPage(P provider) throws Exception;

    /**
     * returns the total amount of items in the non-paged result set. In some scenarios,
     * it might not be possible/convenient to actually retrieve this value. -1 is
     * returned in such a case.
     *
     * @param provider The provider to be used to do the query. You can assume this provider is already properly initialised
     */
    public abstract int getTotalResults(P provider) throws Exception;


}

