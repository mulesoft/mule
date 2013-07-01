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

import java.util.NoSuchElementException;

public interface Consumer<T> extends Closeable
{

    /**
     * Retrieves the next available item.
     * 
     * @return an object of type T if available
     * @throws NoSuchElementException if no more items are available
     */
    public T consume() throws NoSuchElementException;

    /**
     * Returns <code>true</code> if no more items are available. When the resource
     * has been fully consumed and this method returns <code>true</code>,
     * implementors of this class are require to invoke the {@link
     * org.mule.api.Closeable.close()} method before returning in order to release
     * resources as quickly as possible. Users of this component are still required
     * to invoke the same close method when they're finish with it. This is so to
     * account for the case in which a consumer needs to be released before being
     * fully consumed
     * 
     * @return <code>true</code> if no more items are available. <code>false</code>
     *         otherwise
     */
    public boolean isConsumed();

}
