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
 * A producer implementation that follows the idea of the Producer-Consumer design
 * pattern. Implementing this interface does not guarantee thread safeness. Check
 * each particular implementation for information about that
 */
public interface Producer<T> extends Closeable
{

    /**
     * Returns a list with all available items at the moment of invoking
     * 
     * @return a list of items. Might be null or empty depending on implementation
     */
    public List<T> produce();

    /**
     * returns the total amount of items available for consumption. In some
     * scenarios, it might not be possible/convenient to actually retrieve this
     * value. -1 is returned in such a case.
     */
    public int totalAvailable();

}
