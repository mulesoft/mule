/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.streaming;

import org.mule.api.Closeable;

import java.util.List;

public interface PagingDelegate<T> extends Closeable
{

    public List<T> getPage();

    /**
     * returns the total amount of items in the unpaged resultset. In some scenarios,
     * it might not be possible/convenient to actually retrieve this value. -1 is
     * returned in such a case.
     */
    public int getTotalResults();

}
